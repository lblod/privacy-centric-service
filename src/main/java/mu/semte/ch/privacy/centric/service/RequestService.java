package mu.semte.ch.privacy.centric.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jasminb.jsonapi.JSONAPIDocument;
import com.github.jasminb.jsonapi.ResourceConverter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.lib.utils.ModelUtils;
import mu.semte.ch.lib.utils.SparqlClient;
import mu.semte.ch.lib.utils.SparqlQueryStore;
import mu.semte.ch.privacy.centric.jsonapi.Gender;
import mu.semte.ch.privacy.centric.jsonapi.Nationality;
import mu.semte.ch.privacy.centric.jsonapi.PersonInformationAsk;
import mu.semte.ch.privacy.centric.jsonapi.PersonInformationRequest;
import mu.semte.ch.privacy.centric.jsonapi.PersonInformationUpdate;
import mu.semte.ch.privacy.centric.jsonapi.ValidateSsn;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Optional.ofNullable;
import static mu.semte.ch.lib.utils.ModelUtils.formattedDate;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;


@Service
@Slf4j
public class RequestService {
  @Value("${sparql.sessionGraphUri}")
  private String sessionGraphUri;
  @Value("${sparql.defaultGraphUri}")
  private String graph;
  @Value("${sparql.appGraphUri}")
  private String appGraph;

  private final SparqlQueryStore queryStore;
  private final SparqlClient sparqlClient;
  private final ResourceConverter resourceConverter;

  public RequestService(SparqlQueryStore queryStore,
                        SparqlClient sparqlClient, ResourceConverter resourceConverter) {
    this.queryStore = queryStore;
    this.sparqlClient = sparqlClient;
    this.resourceConverter = resourceConverter;
  }

  public void processUpdate(String payload, String sessionId) {
    var document = resourceConverter.readDocument(checkIdOrCreate(payload).getBytes(StandardCharsets.UTF_8), PersonInformationUpdate.class);
    var updateRequest = ofNullable(document.get()).orElseThrow(() -> new RuntimeException("could not parse request"));
    checkNotNull(updateRequest.getPerson(), "Person must be set!");
    checkNotNull(updateRequest.getReason(), "Reason must be set!");
    String reasonId = updateRequest.getReason().getId();
    String personId = updateRequest.getPerson().getId();
    String accountUri = getAccountBySession(sessionId);
    checkNotNull(personId, "Person id must be set!");
    checkNotNull(reasonId, "Reason must be set!");
    checkNotNull(accountUri, "Account must be set!");
    Map<String, Object> parameters = new HashMap<>();
    if (updateRequest.getNationalities() != null && !updateRequest.getNationalities().isEmpty()) {
      var nationaliteits = updateRequest.getNationalities().stream().map(nationality -> {
                                          var q = queryStore.getQueryWithParameters("getNationalityById", Map.of("nationalityId", nationality.getId()));
                                          List<Map<String, String>> results = sparqlClient.executeSelectQueryAsListMap(q);
                                          if (results.isEmpty()) {
                                            return null;
                                          }
                                          return results.get(0).get("nationality");
                                        })
                                        .filter(Objects::nonNull)
                                        .collect(Collectors.toList());
      parameters.put("nationaliteits", nationaliteits);
    }
    if (updateRequest.getGender() != null && isNotEmpty(updateRequest.getGender().getId())) {
      var q = queryStore.getQueryWithParameters("getGenderById", Map.of("genderId", updateRequest.getGender().getId()));
      List<Map<String, String>> results = sparqlClient.executeSelectQueryAsListMap(q);
      if (!results.isEmpty()) {
        String genderUri = results.get(0).get("genderUri");
        if (isNotEmpty(genderUri)) {
          parameters.put("gender", genderUri);
        }
      }
    }

    String registrationNumber = updateRequest.getRegistrationNumber();
    if (isNotEmpty(registrationNumber)) {
      if (!validateSsn(personId, registrationNumber)) {
        throw new RuntimeException("Registration number '%s' doesn't belong to person with id '%s'".formatted(registrationNumber, personId));
      }
      parameters.put("registration", registrationNumber);
    }
    if (isNotEmpty(updateRequest.getDateOfBirth())) {
      parameters.put("dateOfBirth", updateRequest.getDateOfBirth());
    }

    var deleteDataPerson = queryStore.getQueryWithParameters("deleteDataPerson", Map.of(
            "personId", personId,
            "graph", graph
    ));
    sparqlClient.executeUpdateQuery(deleteDataPerson);

    if (!parameters.isEmpty()) {
      String reasonUri = getReasonUri(reasonId);
      parameters.put("personId", personId);
      parameters.put("graph", graph);
      parameters.put("time", formattedDate(LocalDateTime.now()));
      parameters.put("accountUri", accountUri);
      parameters.put("code", reasonUri);
      var insertDataPerson = queryStore.getQueryWithParameters("insertDataPerson", parameters);
      sparqlClient.executeUpdateQuery(insertDataPerson);
    }
  }

  @SneakyThrows
  public String processCheckPersonInfo(String personId, String sessionId) {
    String accountUri = getAccountBySession(sessionId);
    checkNotNull(accountUri, "Account must be set!");
    checkNotNull(personId, "Person id must be set!");
    checkArgument(isNotEmpty(personId), "Person id must be set!");
    var build = buildPersonInfo(personId).build();
    var responseBuilder = PersonInformationAsk.builder();
    ofNullable(build.getDateOfBirth())
            .filter(StringUtils::isNotEmpty)
            .ifPresent(dateOfBirth -> responseBuilder.dateOfBirth("*".repeat(dateOfBirth.length())));
    ofNullable(build.getRegistrationNumber())
            .filter(StringUtils::isNotEmpty)
            .ifPresent(rn -> responseBuilder.registrationNumber("*".repeat(rn.length())));
    ofNullable(build.getGender())
            .map(Gender::getId)
            .filter(StringUtils::isNotEmpty)
            .ifPresent(genderId -> responseBuilder.gender(Gender.builder().id("*".repeat(genderId.length())).build()));

    List<Nationality> nationalities = ofNullable(build.getNationalities())
            .stream().flatMap(Collection::stream)
            .map(Nationality::getId)
            .filter(StringUtils::isNotEmpty)
            .map(id -> Nationality.builder().id("*".repeat(id.length())).build())
            .collect(Collectors.toList());
    responseBuilder.nationalities(nationalities);

    byte[] responseBytes = resourceConverter.writeDocument(new JSONAPIDocument<>(responseBuilder.build()));
    return new String(responseBytes);
  }

  private PersonInformationRequest.PersonInformationRequestBuilder buildPersonInfo(String personId) {
    var responseBuilder = PersonInformationRequest.builder();
    Map<String, Object> queryParameters = Map.of(
            "graph", this.graph,
            "appGraph", appGraph,
            "personId", personId
    );
    String getPersonInfo = queryStore.getQueryWithParameters("getPersonInfo", queryParameters);

    List<Map<String, String>> res = sparqlClient.executeSelectQueryAsListMap(getPersonInfo);
    if (!res.isEmpty()) {
      Map<String, String> mapRes = res.get(0);
      responseBuilder.dateOfBirth(mapRes.get("dateOfBirth"));
      responseBuilder.registrationNumber(mapRes.get("registrationNumber"));
      String genderId = mapRes.get("genderId");
      if (isNotEmpty(genderId)) {
        responseBuilder.gender(Gender.builder().id(genderId).build());
      }
    }

    String getPersonNationalities = queryStore.getQueryWithParameters("getPersonNationalities", queryParameters);
    List<Map<String, String>> nationaliteits = sparqlClient.executeSelectQueryAsListMap(getPersonNationalities);
    if (!nationaliteits.isEmpty()) {
      responseBuilder.nationalities(nationaliteits.stream()
                                                  .map(n -> n.get("nationaliteitId"))
                                                  .filter(StringUtils::isNotEmpty)
                                                  .map(n -> Nationality.builder().id(n).build())
                                                  .collect(Collectors.toList()));
    }
    return responseBuilder;
  }

  @SneakyThrows
  public String processRead(String payload, String sessionId) {
    var document = resourceConverter.readDocument(checkIdOrCreate(payload).getBytes(StandardCharsets.UTF_8), PersonInformationRequest.class);
    var readRequest = ofNullable(document.get()).orElseThrow(() -> new RuntimeException("could not parse request"));
    checkNotNull(readRequest.getPerson(), "Person must be set!");
    checkNotNull(readRequest.getReason(), "Reason must be set!");
    String reasonId = readRequest.getReason().getId();
    String personId = readRequest.getPerson().getId();
    String accountUri = getAccountBySession(sessionId);
    checkNotNull(personId, "Person id must be set!");
    checkNotNull(reasonId, "Reason must be set!");
    checkNotNull(accountUri, "Account must be set!");

    var responseBuilder = this.buildPersonInfo(personId);

    responseBuilder.reason(readRequest.getReason());

    String reasonUri = getReasonUri(reasonId);
    String requestReadReason = queryStore.getQueryWithParameters("requestReadReason", Map.of(
            "graph", this.graph,
            "appGraph", appGraph,
            "personId", personId,
            "code", reasonUri,
            "time", formattedDate(LocalDateTime.now()),
            "accountUri", accountUri
    ));
    sparqlClient.executeUpdateQuery(requestReadReason);

    byte[] responseBytes = resourceConverter.writeDocument(new JSONAPIDocument<>(responseBuilder.build()));
    return new String(responseBytes);
  }

  private String getReasonUri(String reasonId) {
    var getReasonCodeUri = queryStore.getQueryWithParameters("getReasonById", Map.of("reasonId", reasonId));
    List<Map<String, String>> res = sparqlClient.executeSelectQueryAsListMap(getReasonCodeUri);
    checkArgument(!res.isEmpty(), "Code list not found");
    String reasonUri = res.get(0).get("reasonUri");
    checkArgument(isNotEmpty(reasonUri), "Code list not found");
    return reasonUri;
  }

  @SneakyThrows
  private String checkIdOrCreate(String jsonApiData) {
    var mapper = new ObjectMapper();
    JsonNode jsonNode = mapper.readTree(jsonApiData);
    JsonNode data = jsonNode.get("data");
    JsonNode id = data.get("id");
    if (id == null || StringUtils.isEmpty(id.asText())) {
      ((ObjectNode) data).put("id", ModelUtils.uuid());
    }
    return jsonNode.toString();
  }

  private String getAccountBySession(String sessionId) {
    var query = queryStore.getQueryWithParameters("getAccount", Map.of(
            "sessionId", sessionId,
            "sessionGraphUri", sessionGraphUri
    ));
    return sparqlClient.executeSelectQuery(query, resultSet -> {
      if (resultSet.hasNext()) {
        QuerySolution qs = resultSet.next();
        RDFNode account = qs.get("account");
        if (account != null && account.isResource()) {
          return account.asResource().getURI();
        }
      }
      return null;
    });
  }


  @SneakyThrows
  public String validateSsn(String personId, String ssn, String sessionId) {
    String accountUri = getAccountBySession(sessionId);
    checkNotNull(accountUri, "Account must be set!");
    checkNotNull(personId, "Person id must be set!");
    checkArgument(isNotEmpty(personId), "Person id must be set!");
    checkArgument(isNotEmpty(ssn), "SSN must be set!");
    byte[] responseBytes = resourceConverter.writeDocument(new JSONAPIDocument<>(ValidateSsn.builder()
                                                                                            .isValid(validateSsn(personId, ssn))
                                                                                            .build()));
    return new String(responseBytes);
  }

  private boolean validateSsn(String personId, String ssn) {
    var query = queryStore.getQueryWithParameters("askSsn", Map.of("personId", personId, "ssn", ssn, "graph", graph));
    return !sparqlClient.executeAskQuery(query);
  }
}

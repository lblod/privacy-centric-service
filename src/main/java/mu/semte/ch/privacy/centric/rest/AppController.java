package mu.semte.ch.privacy.centric.rest;

import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.privacy.centric.service.RequestService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import static mu.semte.ch.lib.Constants.HEADER_MU_SESSION_ID;


@RestController
@Slf4j
public class AppController {

  private final RequestService requestService;

  public AppController(RequestService requestService) {
    this.requestService = requestService;
  }

  @PostMapping(value = "/person-information-updates",
               consumes = "application/vnd.api+json")
  public ResponseEntity<Void> personInformationUpdates(@RequestBody String payload, HttpServletRequest request) {
    requestService.processUpdate(payload, getSessionIdHeader(request));
    return ResponseEntity.noContent().build();
  }

  @PostMapping(value = "/person-information-requests",
               consumes = "application/vnd.api+json",
               produces = "application/vnd.api+json")
  public ResponseEntity<String> personInformationRequests(@RequestBody String payload, HttpServletRequest request) {
    return ResponseEntity.ok(requestService.processRead(payload, getSessionIdHeader(request)));
  }

  @PostMapping(value = "/person-information-validate-ssn/{personId}",
               consumes = "application/vnd.api+json",
               produces = "application/vnd.api+json")
  public ResponseEntity<String> validateSsn(@PathVariable("personId") String personId,
                                            @RequestParam("ssn") String ssn,
                                            HttpServletRequest request) {
    return ResponseEntity.ok(requestService.validateSsn(personId, ssn, getSessionIdHeader(request)));
  }

  @PostMapping(value = "/person-information-ask/{personId}",
               produces = "application/vnd.api+json")
  public ResponseEntity<String> askPersonInfo(@PathVariable("personId") String personId, HttpServletRequest request) {
    return ResponseEntity.ok(requestService.processCheckPersonInfo(personId, getSessionIdHeader(request)));
  }

  private String getSessionIdHeader(HttpServletRequest request) {
    String sessionIdHeader = request.getHeader(HEADER_MU_SESSION_ID);
    if (StringUtils.isEmpty(sessionIdHeader)) {
      throw new RuntimeException("Session header is missing");
    }
    return sessionIdHeader;
  }
}

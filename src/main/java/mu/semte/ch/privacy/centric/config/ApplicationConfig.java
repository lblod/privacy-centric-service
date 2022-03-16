package mu.semte.ch.privacy.centric.config;

import com.github.jasminb.jsonapi.ResourceConverter;
import lombok.extern.slf4j.Slf4j;
import mu.semte.ch.lib.config.CoreConfig;
import mu.semte.ch.privacy.centric.jsonapi.Gender;
import mu.semte.ch.privacy.centric.jsonapi.Nationality;
import mu.semte.ch.privacy.centric.jsonapi.Person;
import mu.semte.ch.privacy.centric.jsonapi.PersonInformationAsk;
import mu.semte.ch.privacy.centric.jsonapi.PersonInformationRequest;
import mu.semte.ch.privacy.centric.jsonapi.PersonInformationUpdate;
import mu.semte.ch.privacy.centric.jsonapi.RequestReason;
import mu.semte.ch.privacy.centric.jsonapi.ValidateSsn;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(CoreConfig.class)
@Slf4j
public class ApplicationConfig {

  @Value("${sparql.baseUrl}")
  private String baseUrl;

  @Bean
  public ResourceConverter resourceConverter() {
    return new ResourceConverter(baseUrl,
                                 Gender.class, Nationality.class, Person.class, PersonInformationUpdate.class,
                                 PersonInformationRequest.class,
                                 PersonInformationAsk.class,
                                 ValidateSsn.class,
                                 RequestReason.class);
  }

}

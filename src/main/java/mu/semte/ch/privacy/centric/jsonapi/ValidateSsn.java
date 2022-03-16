package mu.semte.ch.privacy.centric.jsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jasminb.jsonapi.annotations.Id;
import com.github.jasminb.jsonapi.annotations.Type;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mu.semte.ch.lib.utils.ModelUtils;

@Builder(toBuilder = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Type("validate-ssn")
public class ValidateSsn {

  @Builder.Default
  @Id
  private String id = ModelUtils.uuid();
  @JsonProperty("is-valid")
  private boolean isValid;


}

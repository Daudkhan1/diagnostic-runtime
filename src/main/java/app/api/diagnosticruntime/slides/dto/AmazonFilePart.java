package app.api.diagnosticruntime.slides.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AmazonFilePart {

    @JsonProperty("etag")
    private String etag;

    @JsonProperty("partNumber")
    private Integer partNumber;
}

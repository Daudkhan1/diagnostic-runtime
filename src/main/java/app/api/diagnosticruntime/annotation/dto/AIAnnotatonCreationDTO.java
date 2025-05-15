package app.api.diagnosticruntime.annotation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AIAnnotatonCreationDTO {

    private String patient_slide_id;
    private String annotation_type;
    private List<AIAnnotationCreationData> annotations;

}

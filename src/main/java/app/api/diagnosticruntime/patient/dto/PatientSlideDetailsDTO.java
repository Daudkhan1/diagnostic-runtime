package app.api.diagnosticruntime.patient.dto;

import app.api.diagnosticruntime.annotation.dto.AnnotatorDetailsDTO;
import app.api.diagnosticruntime.annotation.model.Annotation;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class PatientSlideDetailsDTO {
    private String status;
    private long annotations;
    private long aiAnnotations;
    private String caseId;
    private String organ;
    private List<AnnotatorDetailsDTO> annotationsDetails;

    private void setAnnotationDetails(List<Annotation> annotations) {
        Map<String, List<Annotation>> annotationsByUser = annotations.stream()
            .collect(Collectors.groupingBy(
                Annotation::getCreatedBy,
                Collectors.toList()
            ));
        // ... rest of the method
    }
}

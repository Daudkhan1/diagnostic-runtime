package app.api.diagnosticruntime.annotation.dto;

import lombok.Data;

@Data
public class AnnotatorDetailsDTO {
    private String annotatedBy;
    private String role;
    private int annotationCount;
}

package app.api.diagnosticruntime.annotation.dto;

import app.api.diagnosticruntime.annotation.model.AnnotationState;
import lombok.Data;

@Data
public class AnnotationDetailsDTO {
    private String id;
    private String name;
    private String shape;
    private String description;
    private String biologicalType;
    private String annotationType;
    private String annotatedBy;
    private String diseaseSpectrum;
    private String subType;
    private String grading;
    private Float confidence;
    private String role;
    private AnnotationState status;
}

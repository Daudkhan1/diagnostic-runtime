package app.api.diagnosticruntime.annotation.dto;

import app.api.diagnosticruntime.annotation.model.AnnotationType;
import app.api.diagnosticruntime.annotation.model.Coordinate;
import app.api.diagnosticruntime.annotation.model.ManualCoordinate;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.annotation.model.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AnnotationDTO {
    private String id;
    private String patientSlideId;
    private AnnotationType annotationType;
    private String name;
    private String biologicalType;
    private CaseType caseType;
    private String shape;
    private String description;
    private Set<Coordinate> coordinates;
    private Set<ManualCoordinate> manualCoordinates;
    private String annotatorCoordinates;
    private String color;
    private AnnotationState state;
    private Boolean isDeleted = false;
    private String lastModifiedUser;
    private DiseaseSpectrumDTO diseaseSpectrum;
    private SubtypeDTO subtype;
    private GradingDTO grading;
}

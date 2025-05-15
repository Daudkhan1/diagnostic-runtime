package app.api.diagnosticruntime.annotation.dto;

import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnotationUpdateDTO {
    private String biologicalType;
    private String description;
    private DiseaseSpectrumDTO diseaseSpectrum;
    private SubtypeDTO subtype;
    private GradingDTO grading;
}

package app.api.diagnosticruntime.disease.dto;

import lombok.Data;

@Data
public class DiseaseSpectrumDTO {
    private String id;
    private String name;
    private String organName;  // For frontend display
} 
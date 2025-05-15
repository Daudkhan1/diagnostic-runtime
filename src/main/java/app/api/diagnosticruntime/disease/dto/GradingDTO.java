package app.api.diagnosticruntime.disease.dto;

import lombok.Data;

@Data
public class GradingDTO {
    private String id;
    private String name;
    private String diseaseSpectrumName;
    private String organName;
} 
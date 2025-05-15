package app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto;

import lombok.Data;

@Data
public class DiagnosisUpdateDTO {
    private String caseId;
    private String gross;
    private String microscopy;
    private String diagnosis;
}

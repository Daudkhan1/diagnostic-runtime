package app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto;

import lombok.Data;

@Data
public class DiagnosisDTO {
    private String id;
    private String caseId;
    private String gross;
    private String microscopy;
    private String diagnosis;
    private String userId;
    private boolean isDeleted;
}

package app.api.diagnosticruntime.patient.casemanagment.history.dto;

import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusCount {
    private CaseStatus status;
    private String actionBy;
    private String transferredTo;
    private long count;
}

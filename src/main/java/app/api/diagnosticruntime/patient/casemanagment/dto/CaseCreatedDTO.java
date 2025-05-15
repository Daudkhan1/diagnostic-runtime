package app.api.diagnosticruntime.patient.casemanagment.dto;

import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CaseCreatedDTO {
    private String id;

    private String name;

    private CaseStatus status;

    private LocalDateTime date;

    private CaseType caseType;

    private String patientId;

    private boolean isDeleted;
}

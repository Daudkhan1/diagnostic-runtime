package app.api.diagnosticruntime.patient.casemanagment.dto;

import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class CaseInfoDTO {
    private long total;
    private long newCases;
    private long inProgress;
    private long completed;
    private long referred;
    private long incoming;
}

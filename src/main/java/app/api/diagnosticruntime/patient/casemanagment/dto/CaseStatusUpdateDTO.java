package app.api.diagnosticruntime.patient.casemanagment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CaseStatusUpdateDTO {
    private String status;
}

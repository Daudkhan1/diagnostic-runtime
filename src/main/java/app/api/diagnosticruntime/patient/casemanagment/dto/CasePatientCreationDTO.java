package app.api.diagnosticruntime.patient.casemanagment.dto;

import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CasePatientCreationDTO {
    private PatientDetailsDTO patientDetailsDTO;
    private CaseType caseType;
}

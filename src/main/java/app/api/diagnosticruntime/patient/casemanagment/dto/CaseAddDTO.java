package app.api.diagnosticruntime.patient.casemanagment.dto;

import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideAddDTO;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CaseAddDTO {
    private String id;
    private String caseName;
    private CaseStatus status;
    private LocalDateTime date;
    private CaseType caseType;
    private PatientDetailsDTO patientDetailsDTO;
    private List<PatientSlideAddDTO> patientSlides;

}

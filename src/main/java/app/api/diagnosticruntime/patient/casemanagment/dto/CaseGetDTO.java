package app.api.diagnosticruntime.patient.casemanagment.dto;

import app.api.diagnosticruntime.patient.casemanagment.comment.CommentDTO;
import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideDTO;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.history.dto.TransferDetailsDTO;
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
public class CaseGetDTO {
    private String id;
    private String caseName;
    private CaseStatus status;
    private LocalDateTime date;
    private CaseType caseType;
    private PatientDetailsDTO patientDetailsDTO;
    private List<PatientSlideDTO> slides;
    private List<CommentDTO> comments;
    private Boolean incoming = false;
    private TransferDetailsDTO transferDetails;
}

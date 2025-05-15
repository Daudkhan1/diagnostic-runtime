package app.api.diagnosticruntime.patient.casemanagment.mapper;

import app.api.diagnosticruntime.patient.casemanagment.comment.Comment;
import app.api.diagnosticruntime.patient.casemanagment.comment.CommentDTO;
import app.api.diagnosticruntime.patient.casemanagment.comment.CommentMapper;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseCreatedDTO;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseGetDTO;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideDTO;
import app.api.diagnosticruntime.patient.mapper.PatientSlideMapper;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static app.api.diagnosticruntime.patient.mapper.PatientMapper.toPatientDetailsFromPatient;

@Component
@RequiredArgsConstructor
public class CaseMapper {

    private final PatientSlideMapper patientSlideMapper;


    public static CaseCreatedDTO toCaseCreatedDTO(Case caseEntity){
        CaseCreatedDTO caseCreatedDTO = new CaseCreatedDTO();
        caseCreatedDTO.setId(caseEntity.getId());
        caseCreatedDTO.setName(caseEntity.getName());
        caseCreatedDTO.setStatus(caseEntity.getStatus());
        caseCreatedDTO.setDate(caseEntity.getDate());
        caseCreatedDTO.setCaseType(caseEntity.getCaseType());
        caseCreatedDTO.setPatientId(caseEntity.getPatientId());
        caseCreatedDTO.setDeleted(caseEntity.isDeleted());
        return caseCreatedDTO;
    }

    public CaseGetDTO toCaseGetDTO(Case caseEntity, Patient patient, List<PatientSlide> patientSlides, List<Comment> comments, Boolean incoming) {
        CaseGetDTO caseGetDTO = new CaseGetDTO();

        if (patient != null) {
            PatientDetailsDTO patientDetailsDTO = toPatientDetailsFromPatient(patient);
            caseGetDTO.setPatientDetailsDTO(patientDetailsDTO);
        }

        caseGetDTO.setId(caseEntity.getId());
        caseGetDTO.setCaseName(caseEntity.getName());
        caseGetDTO.setStatus(caseEntity.getStatus());
        caseGetDTO.setDate(caseEntity.getDate());
        caseGetDTO.setCaseType(caseEntity.getCaseType());
        caseGetDTO.setIncoming(incoming);

        if (patientSlides != null) {
            List<PatientSlideDTO> slideDTOs = patientSlides.stream().map(patientSlideMapper::toPatientSlideDTO).collect(Collectors.toList());
            caseGetDTO.setSlides(slideDTOs);
        }

        if(comments != null) {
            List<CommentDTO> commentDTOS = comments.stream().map(CommentMapper::toCommentDTO).toList();
            caseGetDTO.setComments(commentDTOS);
        }

        return caseGetDTO;
    }
}


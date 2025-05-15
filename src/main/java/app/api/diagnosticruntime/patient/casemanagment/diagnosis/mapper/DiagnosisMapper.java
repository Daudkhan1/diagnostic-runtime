package app.api.diagnosticruntime.patient.casemanagment.diagnosis.mapper;


import app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto.DiagnosisDTO;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.model.Diagnosis;

public class DiagnosisMapper {

    public static DiagnosisDTO toDTO(Diagnosis diagnosis) {
        DiagnosisDTO dto = new DiagnosisDTO();
        dto.setId(diagnosis.getId());
        dto.setCaseId(diagnosis.getCaseId());
        dto.setGross(diagnosis.getGross());
        dto.setMicroscopy(diagnosis.getMicroscopy());
        dto.setDiagnosis(diagnosis.getDiagnosis());
        dto.setUserId(diagnosis.getUserId());
        dto.setDeleted(diagnosis.isDeleted());
        return dto;
    }

    public static Diagnosis toEntity(DiagnosisDTO dto) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setId(dto.getId());
        diagnosis.setCaseId(dto.getCaseId());
        diagnosis.setGross(dto.getGross());
        diagnosis.setMicroscopy(dto.getMicroscopy());
        diagnosis.setDiagnosis(dto.getDiagnosis());
        diagnosis.setUserId(dto.getUserId());
        diagnosis.setDeleted(dto.isDeleted());
        return diagnosis;
    }
}

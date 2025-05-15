package app.api.diagnosticruntime.patient.mapper;

import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.model.Patient;

public class PatientMapper {

    public static Patient toPatientFromPatientDetails(PatientDetailsDTO patientDetailsDTO) {
        Patient patient = new Patient();
        patient.setId(patientDetailsDTO.getId());
        patient.setGender(patientDetailsDTO.getGender());
        patient.setAge(patientDetailsDTO.getAge());
        patient.setMrn(patientDetailsDTO.getMrn());
        return patient;
    }

    public static PatientDetailsDTO toPatientDetailsFromPatient(Patient patient) {
        PatientDetailsDTO patientDetailsDTO = new PatientDetailsDTO();
        patientDetailsDTO.setId(patient.getId());
        patientDetailsDTO.setGender(patient.getGender());
        patientDetailsDTO.setAge(patient.getAge());
        patientDetailsDTO.setMrn(patient.getDecryptedMrn());
        patientDetailsDTO.setPraidId(patient.getFormattedPraidId());
        return patientDetailsDTO;
    }
}

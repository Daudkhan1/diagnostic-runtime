package app.api.diagnosticruntime.patient.service;

import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.repository.PatientRepository;
import app.api.diagnosticruntime.util.AESUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static app.api.diagnosticruntime.patient.mapper.PatientMapper.toPatientFromPatientDetails;

@Transactional
@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;

    public Long getTotalPatients() {
        return patientRepository.count();
    }

    public Optional<Patient> getPatientByMrnNumber(String mrnNumber) {
        return patientRepository.findByMrn(AESUtil.encrypt(mrnNumber));
    }

    public Optional<Patient> getPatientById(String id) {
        return patientRepository.findById(id);
    }

    public Patient createPatient(PatientDetailsDTO patientDetailsDTO) {
        Patient patient = toPatientFromPatientDetails(patientDetailsDTO);
        return patientRepository.save(patient);
    }

    public static Long extractPraidNumber(String formattedPraidId) {
        if (formattedPraidId == null || !formattedPraidId.matches("PRAID-\\d+")) {
            throw new IllegalArgumentException("Invalid PRAID format. Expected format: PRAID-<number>");
        }

        try {
            return Long.parseLong(formattedPraidId.substring(6));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid PRAID number format", e);
        }
    }

    public Optional<Patient> getPatientByPraidId(String formattedPraidId) {
        if (formattedPraidId == null || !formattedPraidId.startsWith("PRAID-")) {
            return Optional.empty();
        }
        try {
            Long praidNumber = extractPraidNumber(formattedPraidId);
            return patientRepository.findByPraidId(praidNumber);
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}

package app.api.diagnosticruntime.patient.controller;

import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.service.PatientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PatientControllerUnitTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientController patientController;

    @Test
    void shouldReturnPatientWhenMrnExists() {
        // Arrange
        String mrn = "123456789";
        Patient patient = new Patient();
        patient.setId("testId");
        patient.setMrn(mrn);
        patient.setGender("Male");
        patient.setAge(30);
        patient.setPraidId(1L);

        when(patientService.getPatientByMrnNumber(mrn)).thenReturn(Optional.of(patient));

        // Act
        ResponseEntity<Patient> response = patientController.getCaseByMrn(mrn);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(patient.getId(), response.getBody().getId());
        assertEquals(patient.getDecryptedMrn(), response.getBody().getDecryptedMrn());
    }

    @Test
    void shouldReturnNotFoundWhenMrnDoesNotExist() {
        // Arrange
        String mrn = "nonexistent";
        when(patientService.getPatientByMrnNumber(mrn)).thenReturn(Optional.empty());

        // Act
        ResponseEntity<Patient> response = patientController.getCaseByMrn(mrn);

        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }
} 
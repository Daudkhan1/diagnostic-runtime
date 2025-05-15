package app.api.diagnosticruntime.patient.controller;

import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.service.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/mrn/{id}")
    public ResponseEntity<Patient> getCaseByMrn(@PathVariable String id) {
        return patientService.getPatientByMrnNumber(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/praid/{id}")
    public ResponseEntity<Patient> getCaseByPraidId(@PathVariable String id) {
        return patientService.getPatientByPraidId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

package app.api.diagnosticruntime.patient.casemanagment.diagnosis.controller;


import app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto.DiagnosisDTO;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto.DiagnosisUpdateDTO;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.service.DiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    @Autowired
    private DiagnosisService diagnosisService;

    @GetMapping
    public ResponseEntity<List<DiagnosisDTO>> getAllDiagnoses() {
        List<DiagnosisDTO> diagnoses = diagnosisService.getAllDiagnoses();
        return ResponseEntity.ok(diagnoses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiagnosisDTO> getDiagnosisById(@PathVariable String id) {
        DiagnosisDTO diagnosis = diagnosisService.getDiagnosisById(id);
        return ResponseEntity.ok(diagnosis);
    }

    @PostMapping
    public ResponseEntity<DiagnosisDTO> createDiagnosis(@RequestBody DiagnosisDTO diagnosisDTO) {
        DiagnosisDTO createdDiagnosis = diagnosisService.createDiagnosis(diagnosisDTO);
        return ResponseEntity.ok(createdDiagnosis);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiagnosisDTO> updateDiagnosis(@PathVariable String id, DiagnosisUpdateDTO diagnosisUpdateDTO) {
        DiagnosisDTO updatedDiagnosis = diagnosisService.updateDiagnosis(id, diagnosisUpdateDTO);
        return ResponseEntity.ok(updatedDiagnosis);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDiagnosis(@PathVariable String id) {
        diagnosisService.deleteDiagnosis(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}/case/{caseId}")
    public ResponseEntity<List<DiagnosisDTO>> getDiagnosesByUserIdAndCaseId(@PathVariable String userId, @PathVariable String caseId) {
        List<DiagnosisDTO> diagnoses = diagnosisService.getDiagnosesByUserIdAndCaseId(userId, caseId);
        return ResponseEntity.ok(diagnoses);
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<List<DiagnosisDTO>> getDiagnosesByCaseId(@PathVariable String caseId) {
        List<DiagnosisDTO> diagnoses = diagnosisService.getDiagnosesByCaseId(caseId);
        return ResponseEntity.ok(diagnoses);
    }
}

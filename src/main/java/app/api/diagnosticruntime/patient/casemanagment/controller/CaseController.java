package app.api.diagnosticruntime.patient.casemanagment.controller;

import app.api.diagnosticruntime.patient.casemanagment.diagnosis.service.DiagnosisService;
import app.api.diagnosticruntime.patient.casemanagment.dto.*;
import app.api.diagnosticruntime.patient.casemanagment.history.service.HistoryService;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.service.CaseSearchService;
import app.api.diagnosticruntime.patient.casemanagment.service.CaseService;
import app.api.diagnosticruntime.patient.dto.PatientSlidesStatusRequest;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.service.PatientService;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.userdetails.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/case")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "APIs for managing cases")
public class CaseController {
    private final CaseService caseService;
    private final CaseSearchService caseSearchService;
    private final HistoryService historyService;
    private final PatientService patientService;
    private final PatientSlideService patientSlideService;
    private final UserService userService;
    private final DiagnosisService diagnosisService;

    @GetMapping("/{caseId}/report/radiology")
    public ResponseEntity<String> generatePdfForRadiology(@PathVariable String caseId) {
        Case currentCase = caseService.getCase(caseId);
        Patient patient = patientService.getPatientById(currentCase.getPatientId()).orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        String url = diagnosisService.createReport(patient, caseId, currentCase.getCaseType());
        return ResponseEntity.ok(url);
    }

    @GetMapping("/{caseId}/report/pathology")
    public ResponseEntity<String> generatePdfPathology(@PathVariable String caseId) {
        Case currentCase = caseService.getCase(caseId);
        Patient patient = patientService.getPatientById(currentCase.getPatientId()).orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        String url = diagnosisService.createReport(patient, caseId, currentCase.getCaseType());
        return ResponseEntity.ok(url);
    }

    @PostMapping("/patient")
    public ResponseEntity<CaseCreatedDTO> addCase(@RequestBody CasePatientCreationDTO caseData, @AuthenticationPrincipal UserDetails userDetails) {

        Patient patient = patientService.createPatient(caseData.getPatientDetailsDTO());
        CaseCreatedDTO caseCreatedDTO = caseService.addCase(caseData, patient.getId());
        historyService.createNewCase(caseCreatedDTO.getId());
        return new ResponseEntity<>(caseCreatedDTO, HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updateCaseStatus(@PathVariable String id, @RequestBody CaseStatusUpdateDTO caseStatus) {
        caseService.updateCaseStatus(id, caseStatus.getStatus());
        return ResponseEntity.ok("Case Updated Successfully");
    }

    @GetMapping("/{id}/patient/slide/status")
    public ResponseEntity<PatientSlidesStatusRequest> checkCaseSlidesStatus(@PathVariable String id) {
        boolean check = patientSlideService.checkIfAllSlidesOfCaseAreComplete(id);
        if(check)
            return ResponseEntity.ok().body(new PatientSlidesStatusRequest("COMPLETED"));
        return ResponseEntity.ok().body(new PatientSlidesStatusRequest("NOT_COMPLETED"));
    }

    @GetMapping("/status")
    public ResponseEntity<List<CaseStatus>> getAllCaseStatuses() {
        List<CaseStatus> statuses = Arrays.asList(CaseStatus.values());
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    @GetMapping("/type")
    public ResponseEntity<List<CaseType>> getAllCaseTypes() {
        List<CaseType> types = Arrays.asList(CaseType.values());
        return new ResponseEntity<>(types, HttpStatus.OK);
    }

    @GetMapping
    @Operation(summary = "Get filtered cases", description = "Retrieve cases with various filters including date range, organ, gender, and age")
    public ResponseEntity<Page<CaseGetDTO>> getCasesFiltered(
            @Parameter(description = "Case status filter")
            @RequestParam(required = false) CaseStatus status,
            
            @Parameter(description = "Organ filter")
            @RequestParam(required = false) String organ,
            
            @Parameter(description = "Case name filter (case-insensitive)")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Case type filter")
            @RequestParam(required = false) CaseType type,
            
            @Parameter(description = "Medical Record Number")
            @RequestParam(required = false) String mrn,
            
            @Parameter(description = "PRAID identifier")
            @RequestParam(required = false) String praidId,
            
            @Parameter(description = "Start date for filtering (format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "End date for filtering (format: yyyy-MM-dd'T'HH:mm:ss)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "Gender filter (MALE/FEMALE/OTHER)")
            @RequestParam(required = false) String gender,
            
            @Parameter(description = "Minimum age filter")
            @RequestParam(required = false) Integer minAge,
            
            @Parameter(description = "Maximum age filter")
            @RequestParam(required = false) Integer maxAge,
            
            @Parameter(description = "Pagination and sorting parameters")
            Pageable pageable,
            
            @AuthenticationPrincipal UserDetails userDetails) {
        
        // Validate date range if both dates are provided
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        // Validate age range if both ages are provided
        if (minAge != null && maxAge != null && maxAge < minAge) {
            return ResponseEntity.badRequest().build();
        }
        
        String loggedInUserId = userService.getUserIdByUsername(userDetails.getUsername());
        Page<CaseGetDTO> cases = caseSearchService.getCasesFiltered(
                status, organ, name, type,
                loggedInUserId, mrn, praidId,
                startDate, endDate,
                gender, minAge, maxAge,
                pageable);
        
        return new ResponseEntity<>(cases, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseGetDTO> getCaseById(@PathVariable String id) {
        return caseService.getCaseById(id)
                .map(c -> caseSearchService.caseGetDTO(c, false))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        return caseService.deleteCase(id, userDetails.getUsername()) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/counts")
    public Map<String, Map<CaseStatus, Long>> getCaseCountsPerMonth() {
        return caseService.getCaseCountsByMonth();
    }
}


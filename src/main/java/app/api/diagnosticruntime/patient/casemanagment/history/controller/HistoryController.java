package app.api.diagnosticruntime.patient.casemanagment.history.controller;

import app.api.diagnosticruntime.patient.casemanagment.dto.CaseInfoDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.dto.TransferDetailsDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.patient.casemanagment.history.service.HistoryService;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/case")
@RequiredArgsConstructor
@Tag(name = "Case Management", description = "APIs for managing case reviews and transitions")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping("/{caseId}/history")
    @Operation(summary = "Get case history", description = "Retrieve the complete state transition history for a case")
    public ResponseEntity<Page<History>> getCaseHistory(
            @PathVariable String caseId,
            Pageable pageable) {
        return ResponseEntity.ok(historyService.getCaseHistory(caseId, pageable));
    }

    @PostMapping("/{caseId}/assign")
    @Operation(summary = "Assign case", description = "Assign a case to the current pathologist")
    public ResponseEntity<Void> assignCase(
            @PathVariable String caseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        historyService.assignCase(caseId, userDetails);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{caseId}/unassign")
    @Operation(summary = "Unassign case", description = "Unassign a case from the current pathologist")
    public ResponseEntity<History> unassignCase(
            @PathVariable String caseId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(name = "deleteAnnotationsAndComments", required = false, defaultValue = "true") boolean deleteAnnotationsAndComments){
        historyService.unassignCase(caseId, userDetails, deleteAnnotationsAndComments);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{caseId}/transfer")
    @Operation(summary = "Transfer case", description = "Transfer a case to another pathologist")
    public ResponseEntity<History> transferCase(
            @PathVariable String caseId,
            @RequestParam String targetPathologistId,
            @AuthenticationPrincipal UserDetails userDetails) {
        historyService.transferCase(caseId, userDetails,targetPathologistId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{caseId}/complete")
    @Operation(summary = "Complete case", description = "Mark a case as complete after second review")
    public ResponseEntity<History> completeCase(
            @PathVariable String caseId,
            @AuthenticationPrincipal UserDetails userDetails) {
        historyService.completeCase(caseId, userDetails);;
        return ResponseEntity.ok().build();
    }

    @GetMapping("/type/{caseType}/status/count")
    public ResponseEntity<CaseInfoDTO> getCaseInfoByType(@PathVariable CaseType caseType, @AuthenticationPrincipal UserDetails userDetails) {
        CaseInfoDTO counts = historyService.getCaseInfoByTypeAndUser(caseType, userDetails.getUsername());
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/{caseId}/latest-status")
    @Operation(summary = "Get latest status", description = "Get the most recent state transition for a case")
    public ResponseEntity<History> getLatestStatus(@PathVariable String caseId) {
        History latestHistory = historyService.getLatestHistoryForCase(caseId);
        return ResponseEntity.ok(latestHistory);
    }

    @GetMapping("/{caseId}/latest-transfer-status")
    @Operation(summary = "Get latest transfer history", description = "Get the most recent transfer details for a case")
    public ResponseEntity<TransferDetailsDTO> getLatestTransferHistory(@PathVariable String caseId) {
        TransferDetailsDTO latestTransferHistory = historyService.getLatestTransferHistory(caseId);
        return ResponseEntity.ok(latestTransferHistory);
    }
}

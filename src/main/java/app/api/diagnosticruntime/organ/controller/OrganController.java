package app.api.diagnosticruntime.organ.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.common.exception.ResourceNotFoundException;
import app.api.diagnosticruntime.organ.model.Organ;
import app.api.diagnosticruntime.organ.service.OrganService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organ")
@RequiredArgsConstructor
public class OrganController {
    private final OrganService organService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<String>>> getAllOrganNames() {
        List<String> organs = organService.getAllOrganNames();
        if (organs.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(organs, "No organs found"));
        }
        return ResponseEntity.ok(ApiResponse.success(organs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Organ>> getOrgan(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(
            organService.getOrgan(id)
                .orElseThrow(() -> new ResourceNotFoundException("Organ not found with id: " + id))
        ));
    }
} 
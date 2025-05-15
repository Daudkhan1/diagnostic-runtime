package app.api.diagnosticruntime.disease.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.service.SubtypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subtype")
@RequiredArgsConstructor
public class SubtypeController {
    private final SubtypeService subtypeService;

    @GetMapping("/organ/{organName}")
    public ResponseEntity<ApiResponse<List<SubtypeDTO>>> getAllByOrgan(@PathVariable String organName) {
        List<SubtypeDTO> subtypes = subtypeService.getAllByOrgan(organName);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subtypes retrieved successfully", subtypes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubtypeDTO>> getById(@PathVariable String id) {
        SubtypeDTO subtype = subtypeService.getSubtypeById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Subtype retrieved successfully", subtype));
    }
} 
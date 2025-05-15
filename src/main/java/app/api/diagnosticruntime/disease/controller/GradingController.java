package app.api.diagnosticruntime.disease.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.service.GradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grading")
@RequiredArgsConstructor
public class GradingController {
    private final GradingService gradingService;

    @GetMapping("/organ/{organName}")
    public ResponseEntity<ApiResponse<List<GradingDTO>>> getAllByOrgan(@PathVariable String organName) {
        List<GradingDTO> gradings = gradingService.getAllByOrgan(organName);
        return ResponseEntity.ok(new ApiResponse<>(true, "Gradings retrieved successfully", gradings));
    }

    @GetMapping("/organ/{organName}/disease-spectrum/{diseaseSpectrum}")
    public ResponseEntity<ApiResponse<List<GradingDTO>>> getAllByOrganAndDiseaseSpectrum(@PathVariable String organName,
                                                                                         @PathVariable String diseaseSpectrum) {
        List<GradingDTO> gradings = gradingService.getAllByOrganAndDiseaseSpectrum(organName, diseaseSpectrum);
        return ResponseEntity.ok(new ApiResponse<>(true, "Gradings retrieved successfully", gradings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GradingDTO>> getById(@PathVariable String id) {
        GradingDTO grading = gradingService.getGradingById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Grading retrieved successfully", grading));
    }
} 
package app.api.diagnosticruntime.disease.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.service.DiseaseSpectrumService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disease-spectrum")
@RequiredArgsConstructor
public class DiseaseSpectrumController {
    private final DiseaseSpectrumService diseaseSpectrumService;

    @GetMapping("/organ/{organName}")
    public ResponseEntity<ApiResponse<List<DiseaseSpectrumDTO>>> getAllByOrgan(@PathVariable String organName) {
        List<DiseaseSpectrumDTO> diseaseSpectrums = diseaseSpectrumService.getAllByOrgan(organName);
        return ResponseEntity.ok(new ApiResponse<>(true, "Disease spectrums retrieved successfully", diseaseSpectrums));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DiseaseSpectrumDTO>> getById(@PathVariable String id) {
        DiseaseSpectrumDTO diseaseSpectrum = diseaseSpectrumService.getDiseaseSpectrumById(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Disease spectrum retrieved successfully", diseaseSpectrum));
    }
} 
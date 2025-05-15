package app.api.diagnosticruntime.disease.service;

import app.api.diagnosticruntime.common.exception.ResourceNotFoundException;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.model.Grading;
import app.api.diagnosticruntime.disease.repository.GradingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradingService {
    private final GradingRepository gradingRepository;
    private final DiseaseSpectrumService diseaseSpectrumService;

    @Transactional
    public GradingDTO createGrading(GradingDTO gradingDTO) {
        if (gradingDTO == null) {
            return null;
        }

        DiseaseSpectrumDTO spectrumDTO = diseaseSpectrumService.findByNameAndOrganName(gradingDTO.getDiseaseSpectrumName(), gradingDTO.getOrganName());

        // Check if grading already exists for this disease spectrum and name
        Optional<Grading> existingGrading = gradingRepository
            .findByNameAndDiseaseSpectrumId(gradingDTO.getName(), spectrumDTO.getId());
        
        if (existingGrading.isPresent()) {
            return toDTO(existingGrading.get());
        }

        // Create new grading
        Grading grading = new Grading();
        grading.setName(gradingDTO.getName());
        grading.setDiseaseSpectrumId(spectrumDTO.getId());

        Grading savedGrading = gradingRepository.save(grading);
        return toDTO(savedGrading);
    }

    @Transactional(readOnly = true)
    public GradingDTO getGradingById(String id) {
        if (id == null) {
            return null;
        }
        return gradingRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Grading not found"));
    }

    @Transactional(readOnly = true)
    public List<GradingDTO> getAllByOrgan(String organName) {
        List<DiseaseSpectrumDTO> diseaseSpectrums = diseaseSpectrumService.getAllByOrgan(organName);
        List<String> diseaseSpectrumIds = diseaseSpectrums.stream().map(DiseaseSpectrumDTO::getId).toList();
        List<Grading> gradings = gradingRepository.findAllByDiseaseSpectrumIdIn(diseaseSpectrumIds);
        return gradings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GradingDTO> getAllByOrganAndDiseaseSpectrum(String organName, String diseaseSpectrumName) {
        DiseaseSpectrumDTO diseaseSpectrum = diseaseSpectrumService.findByNameAndOrganName(diseaseSpectrumName, organName);
        List<Grading> gradings = gradingRepository.findByDiseaseSpectrumId(diseaseSpectrum.getId());
        return gradings.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private GradingDTO toDTO(Grading grading) {

        DiseaseSpectrumDTO spectrumDTO = diseaseSpectrumService.getDiseaseSpectrumById(grading.getDiseaseSpectrumId());

        GradingDTO dto = new GradingDTO();
        dto.setId(grading.getId());
        dto.setName(grading.getName());
        dto.setDiseaseSpectrumName(spectrumDTO.getName());
        dto.setOrganName(spectrumDTO.getOrganName());
        return dto;
    }


}
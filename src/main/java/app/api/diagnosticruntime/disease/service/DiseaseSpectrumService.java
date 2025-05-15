package app.api.diagnosticruntime.disease.service;

import app.api.diagnosticruntime.common.exception.ResourceNotFoundException;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.model.DiseaseSpectrum;
import app.api.diagnosticruntime.disease.repository.DiseaseSpectrumRepository;
import app.api.diagnosticruntime.organ.service.OrganService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DiseaseSpectrumService {
    private final DiseaseSpectrumRepository diseaseSpectrumRepository;
    private final OrganService organService;
    private final MongoTemplate mongoTemplate;

    @Transactional
    public DiseaseSpectrumDTO createOrGetDiseaseSpectrum(DiseaseSpectrumDTO diseaseSpectrumDTO) {
        if (diseaseSpectrumDTO == null) {
            return null;
        }

        // Check if disease spectrum already exists for this organ and name

        String organId = organService.getOrganIdByName(diseaseSpectrumDTO.getOrganName());

        Optional<DiseaseSpectrum> existingSpectrum = diseaseSpectrumRepository
            .findByNameAndOrganId(diseaseSpectrumDTO.getName(), organId);
        
        if (existingSpectrum.isPresent()) {
            return toDTO(existingSpectrum.get());
        }

        // Create new disease spectrum
        DiseaseSpectrum diseaseSpectrum = new DiseaseSpectrum();
        diseaseSpectrum.setName(diseaseSpectrumDTO.getName());
        diseaseSpectrum.setOrganId(organId);

        DiseaseSpectrum savedSpectrum = mongoTemplate.save(diseaseSpectrum);
        return toDTO(savedSpectrum);
    }

    @Transactional(readOnly = true)
    public DiseaseSpectrumDTO getDiseaseSpectrumById(String id) {
        if (Strings.isEmpty(id)) {
            throw new IllegalArgumentException("Disease spectrum ID is empty or null");
        }

        return diseaseSpectrumRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Disease Spectrum not found"));
    }

    @Transactional(readOnly = true)
    public DiseaseSpectrumDTO findByNameAndOrganName(String name, String organName) {
        if (Strings.isEmpty(name)  || Strings.isEmpty(organName)) {
            throw new IllegalArgumentException("Organ name or disease spectrum name is empty or null");
        }

        String organId = organService.getOrganIdByName(organName);
        return diseaseSpectrumRepository.findByNameAndOrganId(name, organId)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Disease Spectrum not found"));
    }

    @Transactional(readOnly = true)
    public List<DiseaseSpectrumDTO> getAllByOrgan(String organName) {
        String organId = organService.getOrganIdByName(organName);
        List<DiseaseSpectrum> diseaseSpectrums = diseaseSpectrumRepository.findByOrganId(organId);
        return diseaseSpectrums.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private DiseaseSpectrumDTO toDTO(DiseaseSpectrum diseaseSpectrum) {
        DiseaseSpectrumDTO dto = new DiseaseSpectrumDTO();
        dto.setId(diseaseSpectrum.getId());
        dto.setName(diseaseSpectrum.getName());
        dto.setOrganName(organService.getOrganNameFromId(diseaseSpectrum.getOrganId()));
        return dto;
    }
} 
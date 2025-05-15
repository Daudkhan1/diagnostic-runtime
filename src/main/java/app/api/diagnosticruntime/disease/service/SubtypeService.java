package app.api.diagnosticruntime.disease.service;

import app.api.diagnosticruntime.common.exception.ResourceNotFoundException;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.model.Subtype;
import app.api.diagnosticruntime.disease.repository.SubtypeRepository;
import app.api.diagnosticruntime.organ.service.OrganService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubtypeService {
    private final SubtypeRepository subtypeRepository;
    private final OrganService organService;


    @Transactional
    public SubtypeDTO createOrGetSubtype(SubtypeDTO subtypeDTO) {
        if (subtypeDTO == null) {
            return null;
        }

        String organId = organService.getOrganIdByName(subtypeDTO.getOrganName());

        // Check if subtype already exists for this organ and name
        Optional<Subtype> existingSubtype = subtypeRepository
            .findByNameAndOrganId(subtypeDTO.getName(), organId);
        
        if (existingSubtype.isPresent()) {
            return toDTO(existingSubtype.get());
        }

        // Create new subtype
        Subtype subtype = new Subtype();
        subtype.setName(subtypeDTO.getName());
        subtype.setOrganId(organId);

        Subtype savedSubtype = subtypeRepository.save(subtype);
        return toDTO(savedSubtype);
    }

    @Transactional(readOnly = true)
    public SubtypeDTO getSubtypeById(String id) {
        if (id == null) {
            return null;
        }
        return subtypeRepository.findById(id)
            .map(this::toDTO)
            .orElseThrow(() -> new ResourceNotFoundException("Subtype not found"));
    }

    @Transactional(readOnly = true)
    public List<SubtypeDTO> getAllByOrgan(String organName) {
        String organId = organService.getOrganIdByName(organName);
        List<Subtype> subtypes = subtypeRepository.findByOrganId(organId);
        return subtypes.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private SubtypeDTO toDTO(Subtype subtype) {

        SubtypeDTO dto = new SubtypeDTO();
        dto.setId(subtype.getId());
        dto.setName(subtype.getName());
        dto.setOrganName(organService.getOrganNameFromId(subtype.getOrganId()));
        return dto;
    }
} 
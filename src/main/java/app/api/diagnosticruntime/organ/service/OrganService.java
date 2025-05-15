package app.api.diagnosticruntime.organ.service;

import app.api.diagnosticruntime.organ.model.Organ;
import app.api.diagnosticruntime.organ.repository.OrganRepository;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class OrganService {
    private final OrganRepository organRepository;

    public String getOrCreateOrganId(String organName) {
        return getOrCreateOrgan(organName)
                .getId();
    }

    public String getOrCreateOrganName(String organName) {
        return getOrCreateOrgan(organName)
                .getName();
    }

    public Organ getOrCreateOrgan(String organName) {
        if (Strings.isEmpty(organName))
            throw new IllegalArgumentException("Organ name cannot be null or empty");
        else {
            String organNameCapitalized = organName.trim().toUpperCase();
            return organRepository.findByName(organNameCapitalized)
                    .orElseGet(() -> {
                        Organ newOrgan = new Organ();
                        newOrgan.setName(organNameCapitalized);
                        return organRepository.save(newOrgan);
                    });
        }
    }



    public String getOrganNameFromId(String organId) {
        return organRepository.findById(organId)
                .map(Organ::getName)
                .orElse(null);
    }

    public String getOrganIdByName(String name) {
        return organRepository.findByName(name)
                .map(Organ::getId)
                .orElse(null);
    }

    public List<Organ> getAllOrgans() {
        return organRepository.findAll();
    }

    public Optional<Organ> getOrgan(String id) {
        return organRepository.findById(id);
    }

    public List<String> getAllOrganNames() {
        return organRepository.findAll().stream()
                .map(Organ::getName)
                .sorted()  // Optional: sort alphabetically
                .collect(Collectors.toList());
    }
} 
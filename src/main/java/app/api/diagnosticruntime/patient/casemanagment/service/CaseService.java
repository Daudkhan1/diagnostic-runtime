package app.api.diagnosticruntime.patient.casemanagment.service;

import app.api.diagnosticruntime.patient.casemanagment.dto.CaseCountResponse;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseCreatedDTO;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseInfoDTO;
import app.api.diagnosticruntime.patient.casemanagment.dto.CasePatientCreationDTO;
import app.api.diagnosticruntime.patient.casemanagment.mapper.CaseMapper;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.repository.CaseRepository;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Transactional
@Service
@RequiredArgsConstructor
public class CaseService {
    private final CaseRepository caseRepository;
    private final PatientSlideService patientSlideService;

    public CaseCountResponse getCaseCount() {
        long totalCases = caseRepository.count();
        long inProgressCases = caseRepository.countByStatus(CaseStatus.IN_PROGRESS);
        long completedCases = caseRepository.countByStatus(CaseStatus.COMPLETE);


        return new CaseCountResponse(totalCases, inProgressCases, completedCases);
    }

    public List<String> getCaseIdsByType(CaseType caseType) {
        return caseRepository.findByCaseTypeAndIsDeleted(caseType, false)
                .stream()
                .map(Case::getId)
                .collect(Collectors.toList());
    }

    public Optional<Case> getCaseById(String id) {
        return caseRepository.findByIdAndIsDeleted(id, false);
    }

    @Transactional(readOnly = true)
    public Case getCase(String id) {
        return caseRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() ->new IllegalArgumentException("Case of id "+id+ " doesn't exist"));
    }

    private String generateUniqueHash() {
        return String.valueOf((int) (Math.random() * 1_000_000_00)); // Generates a random 8-digit number
    }

    public CaseCreatedDTO addCase(CasePatientCreationDTO caseData, String patientId) {

        String uniqueHash = generateUniqueHash();

        Case caseToUpload = new Case();
        caseToUpload.setCaseType(caseData.getCaseType());
        caseToUpload.setName(caseData.getCaseType().name() +"-"+ uniqueHash);
        caseToUpload.setPatientId(patientId);

        caseToUpload.setStatus(CaseStatus.NEW);
        caseToUpload.setDate(LocalDateTime.now());

        caseToUpload.setDeleted(false);
        Case uploadedCase = caseRepository.save(caseToUpload);

        return CaseMapper.toCaseCreatedDTO(uploadedCase);
    }

    public boolean deleteCase(String id, String loggedInUser) {
        Optional<Case> optionalCase = caseRepository.findByIdAndIsDeleted(id, false);
        if (optionalCase.isPresent()) {
            patientSlideService.deleteAllByCaseId(id, loggedInUser);
            Case caseToDelete = optionalCase.get();
            caseToDelete.setDeleted(true);
            caseRepository.save(caseToDelete);
            return true;
        }
        return false;
    }


    public void updateCaseStatus(String id, String caseStatus) {
        CaseStatus status = Optional.ofNullable(caseStatus)
                .map(roleString -> {
                    try {
                        return CaseStatus.valueOf(roleString);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalArgumentException("This status doesn't exist: " + roleString);
                    }
                })
                .orElseThrow(() ->  new IllegalArgumentException("Status cannot be null"));
        Case aCase = caseRepository.findByIdAndIsDeleted(id, false)
                .orElseThrow(() -> new IllegalArgumentException("Case with this ID doesn't exist"));

        if (status.equals(CaseStatus.COMPLETE) && !patientSlideService.checkIfAllSlidesOfCaseAreComplete(aCase.getId())) {
            throw new IllegalArgumentException("Case cannot be completed until all slides are marked as completed");
        }

        aCase.setStatus(status);
        caseRepository.save(aCase);
    }


    public Map<String, Map<CaseStatus, Long>> getCaseCountsByMonth() {
        LocalDate startDate = LocalDate.now().minusMonths(48); 
        LocalDate endDate = LocalDate.now();

        List<Case> cases = caseRepository.findByDateBetween(startDate, endDate);

        return cases.stream()
                .collect(Collectors.groupingBy(
                        c -> "Year: " + c.getDate().getYear() + ", Month: " + c.getDate().getMonth().name(),
                        Collectors.groupingBy(
                                Case::getStatus,
                                Collectors.counting()
                        )
                ));
    }

}


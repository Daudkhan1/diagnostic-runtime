package app.api.diagnosticruntime.patient.casemanagment.service;

import app.api.diagnosticruntime.organ.service.OrganService;
import app.api.diagnosticruntime.patient.casemanagment.comment.Comment;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseGetDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.patient.casemanagment.mapper.CaseMapper;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.dto.PatientSlideDTO;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.util.AESUtil;
import app.api.diagnosticruntime.patient.casemanagment.history.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static app.api.diagnosticruntime.patient.service.PatientService.extractPraidNumber;

@Transactional
@Service
@RequiredArgsConstructor
public class CaseSearchService {

    private final MongoTemplate mongoTemplate;
    private final PatientSlideService patientSlideService;
    private final CaseMapper caseMapper;
    private final OrganService organService;
    private final HistoryService historyService;

    public Page<CaseGetDTO> getCasesFiltered(
            CaseStatus status, String organ, String name, CaseType type,
            String loggedInUserId, String mrn, String praidId, 
            LocalDateTime startDate, LocalDateTime endDate,
            String gender, Integer minAge, Integer maxAge,
            Pageable pageable) {
        
        // Get case IDs based on organ filter from patient slides
        List<String> filteredCaseIds = null;
        if (organ != null && !organ.isEmpty()) {
            String organId = organService.getOrganIdByName(organ.trim().toUpperCase());
            if (organId != null) {
                Query slideQuery = Query.query(Criteria.where("organ_id").is(organId).and("is_deleted").is(false));
                List<PatientSlide> slides = mongoTemplate.find(slideQuery, PatientSlide.class);
                filteredCaseIds = slides.stream()
                        .map(PatientSlide::getCaseId)
                        .distinct()
                        .collect(Collectors.toList());
                
                if (filteredCaseIds.isEmpty()) {
                    return new PageImpl<>(Collections.emptyList(), pageable, 0);
                }
            } else {
                // If organ name doesn't exist, return empty result
                return new PageImpl<>(Collections.emptyList(), pageable, 0);
            }
        }

        // Build case criteria
        List<Criteria> caseCriteria = new ArrayList<>();
        caseCriteria.add(Criteria.where("is_deleted").is(false));

        // Add case-specific filters
        if (status != null) {
            if(status.equals(CaseStatus.IN_COMING))
                caseCriteria.add(Criteria.where("status").is(CaseStatus.REFERRED));
            else
                caseCriteria.add(Criteria.where("status").is(status));
        }
        if (name != null && !name.isEmpty()) {
            caseCriteria.add(Criteria.where("name").regex(name, "i"));
        }
        if (type != null) {
            caseCriteria.add(Criteria.where("case_type").is(type));
        }
        if (startDate != null) {
            caseCriteria.add(Criteria.where("date").gte(startDate));
        }
        if (endDate != null) {
            caseCriteria.add(Criteria.where("date").lte(endDate));
        }

        // Add organ filter case IDs if present
        if (filteredCaseIds != null) {
            caseCriteria.add(Criteria.where("_id").in(filteredCaseIds));
        }

        // Add patient filters
        List<Criteria> patientCriteria = new ArrayList<>();
        if (gender != null && !gender.isEmpty()) {
            patientCriteria.add(Criteria.where("gender").is(gender));
        }
        if (minAge != null) {
            patientCriteria.add(Criteria.where("age").gte(minAge));
        }
        if (maxAge != null) {
            patientCriteria.add(Criteria.where("age").lte(maxAge));
        }
        if (!Strings.isEmpty(mrn)) {
            patientCriteria.add(Criteria.where("mrn").is(AESUtil.encrypt(mrn)));
        }
        if (!Strings.isEmpty(praidId)) {
            patientCriteria.add(Criteria.where("praid").is(extractPraidNumber(praidId)));
        }

        Query patientQuery = Query.query(new Criteria().andOperator(patientCriteria.toArray(new Criteria[0])));
        List<String> patientIds = new ArrayList<>();
        if(!patientCriteria.isEmpty()) {
            List<Patient> patients = mongoTemplate.find(patientQuery, Patient.class);
            patientIds = patients.stream()
                    .map(Patient::getId)
                    .collect(Collectors.toList());
            caseCriteria.add(Criteria.where("patient_id").in(patientIds));
        }

        // Add case state-specific filters
        if (status != null) {
            switch (status) {
                case NEW:
                    // Already handled above
                    break;
                case IN_PROGRESS:
                    List<String> inProgressCaseIds = getCaseIdsByHistory(loggedInUserId, "action_by");
                    caseCriteria.add(Criteria.where("_id").in(inProgressCaseIds));
                    break;
                case REFERRED:
                    List<String> referredCaseIds = getCaseIdsByHistory(loggedInUserId, "action_by");
                    caseCriteria.add(Criteria.where("_id").in(referredCaseIds));
                    break;
                case IN_COMING:
                    List<String> incomingCaseIds = getCaseIdsByHistory(loggedInUserId, "transferred_to");
                    caseCriteria.add(Criteria.where("_id").in(incomingCaseIds));
                    break;
                case COMPLETE:
                    List<String> completeCaseIds = getCaseIdsByHistory(loggedInUserId, "action_by", "transferred_to");
                    caseCriteria.add(Criteria.where("_id").in(completeCaseIds));
                    break;
            }
        }

        // Create the query with all criteria
        Query query = Query.query(new Criteria().andOperator(caseCriteria.toArray(new Criteria[0])))
                .with(pageable);

        // Execute the query
        List<Case> cases = mongoTemplate.find(query, Case.class);
        long total = mongoTemplate.count(query.skip(-1).limit(-1), Case.class);

        // Map to DTOs
        List<CaseGetDTO> caseGetDTOS = cases.stream()
                .map(c -> caseGetDTO(c, status == CaseStatus.IN_COMING))
                .collect(Collectors.toList());

        return new PageImpl<>(caseGetDTOS, pageable, total);
    }

    // Helper class for count aggregation
    private static class CountResult {
        private long total;
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
    }

    public CaseGetDTO caseGetDTO(Case caseEntity, boolean incoming) {
        Patient patient = null;
        if (caseEntity.getPatientId() != null) {
            patient = mongoTemplate.findById(caseEntity.getPatientId(), Patient.class);
        }

        // Fetch PatientSlides
        List<PatientSlide> patientSlides = mongoTemplate.find(
                Query.query(Criteria.where("case_id").is(caseEntity.getId()).and("is_deleted").is(false)),
                PatientSlide.class
        );

        List<PatientSlideDTO> patientSlidesWithCount = patientSlideService.getCountOfAnnotationForPatientSlides(patientSlides);

        // Fetch Comments
        List<Comment> comments = mongoTemplate.find(
                Query.query(Criteria.where("case_id").is(caseEntity.getId())),
                Comment.class
        );

        // Map to CaseDTO
        CaseGetDTO caseGetDTO = caseMapper.toCaseGetDTO(caseEntity, patient, patientSlides, comments, incoming);
        caseGetDTO.setSlides(patientSlidesWithCount);

        // Get transfer details if case is in REFERRED or COMPLETE status
        if (caseEntity.getStatus() == CaseStatus.REFERRED || caseEntity.getStatus() == CaseStatus.COMPLETE ) {
            try {
                caseGetDTO.setTransferDetails(historyService.getLatestTransferHistoryForCaseDisplay(caseEntity.getId()));
            } catch (IllegalArgumentException e) {
                // If no transfer history exists, leave transferDetails as null
            }
        }

        return caseGetDTO;
    }

    private List<String> getCaseIdsByHistory(String loggedInUserId, String... historyFields) {
        List<Criteria> criteriaList = new ArrayList<>();

        for (String field : historyFields) {
            criteriaList.add(Criteria.where(field).is(loggedInUserId));
        }

        Query historyQuery = new Query()
                .addCriteria(new Criteria().orOperator(criteriaList.toArray(new Criteria[0])))
                .with(Sort.by(Sort.Order.desc("created_at")));

        List<History> histories = mongoTemplate.find(historyQuery, History.class);

        return histories.stream()
                .map(History::getCaseId)
                .distinct()
                .collect(Collectors.toList());
    }


}

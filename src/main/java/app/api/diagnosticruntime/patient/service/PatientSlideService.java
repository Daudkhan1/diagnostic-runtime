package app.api.diagnosticruntime.patient.service;

import app.api.diagnosticruntime.annotation.dto.AnnotatorDetailsDTO;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.repository.CaseRepository;
import app.api.diagnosticruntime.patient.dto.PatientSlideAddDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideDetailsDTO;
import app.api.diagnosticruntime.patient.mapper.PatientSlideMapper;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import app.api.diagnosticruntime.patient.repository.PatientSlideRepository;
import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.service.UserService;
import app.api.diagnosticruntime.util.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static app.api.diagnosticruntime.annotation.model.AnnotationType.AI;
import static app.api.diagnosticruntime.annotation.model.AnnotationType.MANUAL;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientSlideService {

    private final PatientSlideRepository patientSlideRepository;
    private final AnnotationService annotationService;
    private final MongoTemplate mongoTemplate;
    private final CaseRepository caseRepository;
    private final UserService userService;
    private final PatientSlideMapper patientSlideMapper;

    @Transactional
    public PatientSlide createPatientSlide(PatientSlideAddDTO patientSlide) {
        String slideImagePath = patientSlide.getSlideImagePath();
        String showImagePath = patientSlide.getShowImagePath();
        patientSlide.setSlideImagePath(FileUtils.toTiffCompliantFileName(slideImagePath));
        patientSlide.setShowImagePath(FileUtils.toTiffCompliantFileName(showImagePath));
        PatientSlide patientSlideToUpload = patientSlideMapper.toPatientSlide(patientSlide);
        patientSlideToUpload.setCreationDate(LocalDate.now());
        patientSlideToUpload.setDeleted(false);
        patientSlideToUpload.setStatus(PatientSlideStatus.NEW);
        return patientSlideRepository.save(patientSlideToUpload);
    }

    public PatientSlideDetailsDTO getSlideDetails(String slideId) {
        PatientSlide slide = patientSlideRepository.findById(slideId).orElseThrow(() -> new IllegalArgumentException("Slide not found"));
        Case caseDetails = caseRepository.findById(slide.getCaseId()).orElseThrow(() -> new IllegalArgumentException("Case not found"));

        List<Annotation> annotations = annotationService.getAllAnnotationsForPatientSlide(slideId);
        long manualAnnotationCount = annotations.stream().filter(annotation -> annotation.getAnnotationType().equals(MANUAL)).count();
        long aiAnnotationCount = annotations.stream().filter(annotation -> annotation.getAnnotationType().equals(AI)).count();

        Map<String, List<Annotation>> annotationsGroupedByUser = annotations.stream()
                .collect(Collectors.groupingBy(
                        annotation -> {
                            String user = annotation.getCreatedBy();
                            return "SYSTEM".equals(user) || "MODEL".equals(user) ? "test@example.com" : user;
                        },
                        Collectors.toList()
                ));


        List<AnnotatorDetailsDTO> annotationDetails = new ArrayList<>();
        annotationsGroupedByUser.keySet().forEach(userName -> {
            User user = userService.getUserByUsername(userName);
            AnnotatorDetailsDTO annotationDTO = new AnnotatorDetailsDTO();
            annotationDTO.setAnnotatedBy(user.getFullName());
            annotationDTO.setRole(user.getRole().toString());
            annotationDTO.setAnnotationCount(annotationsGroupedByUser.get(userName).size());
            annotationDetails.add(annotationDTO);

        });

        return patientSlideMapper.patientSlideDetailsDTOFromPatientSlide(slide, manualAnnotationCount, aiAnnotationCount, caseDetails, annotationDetails);

    }

    @Transactional(readOnly = true)
    public Page<PatientSlideDTO> getAllPatientSlides(String caseId, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        Query query = new Query().with(pageable);

        // Add filtering criteria
        if (caseId != null && !caseId.isEmpty()) {
            query.addCriteria(Criteria.where("case_id").is(caseId));
        }
        if (fromDate != null) {
            query.addCriteria(Criteria.where("creation_date").gte(fromDate));
        }
        if (toDate != null) {
            query.addCriteria(Criteria.where("creation_date").lte(toDate));
        }
        query.addCriteria(Criteria.where("is_deleted").is(false)); // Exclude deleted slides

        // Execute query
        List<PatientSlide> slides = mongoTemplate.find(query, PatientSlide.class);
        long count = mongoTemplate.count(query.skip(-1).limit(-1), PatientSlide.class); // Get total count

        // Map to DTOs
        List<PatientSlideDTO> slideDTOs = getCountOfAnnotationForPatientSlides(slides);

        return new PageImpl<>(slideDTOs, pageable, count);
    }

    public List<PatientSlideDTO> getCountOfAnnotationForPatientSlides(List<PatientSlide> patientSlides) {
        return patientSlides.stream()
                .map(slide -> {
                    Long annotationCount = annotationService.getCountByPatientSlideId(slide.getId());
                    return patientSlideMapper.patientSlideDtoFromPatientSlide(slide, annotationCount);
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PatientSlide> getAllByCaseId(String caseId) {
        return patientSlideRepository.findAllByCaseIdAndIsDeleted(caseId, false);
    }

    @Transactional
    public void updateStatusOfAllCaseSlides(String caseId, PatientSlideStatus patientSlideStatus) {
        List<PatientSlide> patientSlides = getAllByCaseId(caseId);
        patientSlides.forEach(p -> updatePatientStatus(p.getId(), patientSlideStatus));
    }

    @Transactional(readOnly = true)
    public long countAllByCaseId(String caseId) {
        return patientSlideRepository.countAllByCaseIdAndIsDeleted(caseId, false);
    }

    @Transactional(readOnly = true)
    public long countAllByCaseIdAndStatus(String caseId, PatientSlideStatus status) {
        return patientSlideRepository.countAllByCaseIdAndIsDeletedAndStatus(caseId, false, status);
    }

    @Transactional(readOnly = true)
    public boolean checkIfAllSlidesOfCaseAreComplete(String caseId) {
        long allSlidesCount = countAllByCaseId(caseId);
        if(allSlidesCount == 0)
            return true;
        long completedCount = countAllByCaseIdAndStatus(caseId, PatientSlideStatus.COMPLETED);
        if(allSlidesCount == 0 && completedCount == 0)
            return false;
        return allSlidesCount == completedCount;
    }

    @Transactional(readOnly = true)
    public PatientSlide getPatientSlideById(String id) {
        return patientSlideRepository.getPatientSlideByIdAndIsDeleted(id, false)
                .orElseThrow(() ->new IllegalArgumentException("Patient Slide of id "+id+ " doesn't exist"));
    }

    @Transactional
    public void deleteAllByCaseId(String caseId, String loggedInUser) {
        List<PatientSlide> patientSlides = getAllByCaseId(caseId);
        List<String> patientSlideIds = patientSlides.stream().map(PatientSlide::getId).toList();
        annotationService.deleteAllAnnotationsForSlideIdIn(patientSlideIds, loggedInUser);
        // Soft delete patient slides
        patientSlides.forEach(slide -> {
            slide.setDeleted(true);
            patientSlideRepository.save(slide);
        });
    }

    @Transactional
    public void deleteById(String id, String loggedInUser) {
        PatientSlide patientSlide = getPatientSlideById(id);
        patientSlide.setDeleted(true);
        patientSlideRepository.save(patientSlide);
        annotationService.deleteAllAnnotationsForSlide(id, loggedInUser);
    }

    @Transactional
    public void updatePatientStatus(String id, PatientSlideStatus status) {
        patientSlideRepository.getPatientSlideByIdAndIsDeleted(id, false).ifPresent(slide -> {
            slide.setStatus(status);
            patientSlideRepository.save(slide);
        });

    }
}

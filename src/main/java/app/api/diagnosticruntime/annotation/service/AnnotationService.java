package app.api.diagnosticruntime.annotation.service;

import app.api.diagnosticruntime.annotation.ai.repository.BiologicalTypeRepository;
import app.api.diagnosticruntime.annotation.dto.AIAnnotatonCreationDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationUpdateDTO;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.model.AnnotationState;
import app.api.diagnosticruntime.annotation.model.AnnotationType;
import app.api.diagnosticruntime.annotation.model.BiologicalType;
import app.api.diagnosticruntime.annotation.repository.AnnotationRepository;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.service.DiseaseSpectrumService;
import app.api.diagnosticruntime.disease.service.GradingService;
import app.api.diagnosticruntime.disease.service.SubtypeService;
import app.api.diagnosticruntime.organ.service.OrganService;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;


@Service
@RequiredArgsConstructor
public class AnnotationService {
    private final AnnotationRepository annotationRepository;
    private final BiologicalTypeRepository biologicalTypeRepository;
    private final SubtypeService subtypeService;
    private final DiseaseSpectrumService diseaseSpectrumService;
    private final GradingService gradingService;
    private final OrganService organService;

    @Transactional(readOnly = true)
    public Optional<Annotation> findByIdAndSlideId(String id, String slideId) {
        return annotationRepository.findByIdAndPatientSlideIdAndIsDeleted(id, slideId, false);
    }

    @Transactional(readOnly = true)
    public Annotation findById(String id) {
        return annotationRepository.findByIdAndIsDeleted(id, false).orElseThrow(() -> new IllegalArgumentException("Annotation not found"));
    }

    public void createAIAnnotations(AIAnnotatonCreationDTO aiAnnotatonCreationDTO, PatientSlide patientSlide, boolean radiology) {

        StringBuilder organName = new StringBuilder();
        if(!Strings.isEmpty(patientSlide.getOrganId()))
            organName.append(organService.getOrganNameFromId(patientSlide.getOrganId()));


        BiologicalType abnormalRadiologyTypes = checkAndCreateBiologicalType("ABNORMAL", CaseType.RADIOLOGY);
        BiologicalType mitosisMetaphasePathologyTypes = checkAndCreateBiologicalType("MITOSIS_METAPHASE", CaseType.PATHOLOGY);
        List<Annotation> annotations = new ArrayList<>();

        aiAnnotatonCreationDTO.getAnnotations().forEach(aiAnnotationCreationData -> {
            Annotation annotation = new Annotation();

            if(!Strings.isEmpty(organName.toString()))
            {
                SubtypeDTO subtype = new SubtypeDTO();
                subtype.setOrganName(organName.toString());
                subtype.setName(aiAnnotationCreationData.getBiological_type());

                SubtypeDTO createdSubType = subtypeService.createOrGetSubtype(subtype);
                createdSubType.setOrganName(patientSlide.getOrganId());
                annotation.setSubtypeId(createdSubType.getId());
            }

            annotation.setPatientSlideId(patientSlide.getId());

            annotation.setAnnotationType(AnnotationType.AI);
            annotation.setBiologicalType(radiology ? abnormalRadiologyTypes.getName() : mitosisMetaphasePathologyTypes.getName());
            annotation.setConfidence(aiAnnotationCreationData.getConfidence());
            String biologicalTypeName = annotation.getBiologicalType();
            String uniqueHash = generateUniqueHash();
            String generatedName = biologicalTypeName + "_" + uniqueHash;

            annotation.setName(generatedName);
            annotation.setShape(aiAnnotationCreationData.getShape());
            annotation.setDescription(aiAnnotationCreationData.getDescription());
            annotation.setCoordinates(aiAnnotationCreationData.getCoordinates());
            annotation.setColor("#FFFFFF");

            annotation.setDeleted(false);
            annotation.setLastModifiedUser("MODEL");
            annotation.setCreatedBy("MODEL");
            annotation.setState(AnnotationState.NEW);
            annotations.add(annotation);
        });

        annotationRepository.saveAll(annotations);
    }

    @Transactional
    public AnnotationDTO createAnnotationParams(AnnotationDTO annotationDTO) {
        annotationDTO.setDiseaseSpectrum(diseaseSpectrumService.createOrGetDiseaseSpectrum(annotationDTO.getDiseaseSpectrum()));
        annotationDTO.setSubtype(subtypeService.createOrGetSubtype(annotationDTO.getSubtype()));
        annotationDTO.setGrading(gradingService.createGrading(annotationDTO.getGrading()));
        return annotationDTO;
    }

    @Transactional
    public Annotation updateAnnotation(String slideId, String annotationId, AnnotationUpdateDTO updateDTO, CaseType caseType, String loggedInUser) {
        Annotation annotation = annotationRepository.findByIdAndPatientSlideIdAndIsDeleted(annotationId, slideId, false)
                .orElseThrow(() -> new IllegalArgumentException("Annotation not found"));

        // Update the annotation
        annotation.setDescription(updateDTO.getDescription());
        annotation.setLastModifiedUser(loggedInUser);

        DiseaseSpectrumDTO diseaseSpectrum = diseaseSpectrumService.createOrGetDiseaseSpectrum(updateDTO.getDiseaseSpectrum());
        SubtypeDTO subtype = subtypeService.createOrGetSubtype(updateDTO.getSubtype());
        GradingDTO grading = gradingService.createGrading(updateDTO.getGrading());

        if(diseaseSpectrum != null)
            annotation.setDiseaseSpectrumId(diseaseSpectrum.getId());
        if(grading != null)
            annotation.setGradingId(grading.getId());
        if(subtype != null)
            annotation.setSubtypeId(subtype.getId());

        return annotationRepository.save(annotation);
    }

    private BiologicalType checkAndCreateBiologicalType(String biologicalTypeName, CaseType caseType){
        Optional<BiologicalType> biologicalType = biologicalTypeRepository.findByName(biologicalTypeName);
        if(biologicalType.isPresent())
            return biologicalType.get();

        BiologicalType newBiologicalType = new BiologicalType();
        newBiologicalType.setCategory(caseType.toString());
        newBiologicalType.setName(biologicalTypeName);
        return biologicalTypeRepository.save(newBiologicalType);
    }


    @Transactional
    public Annotation createAnnotation(Annotation annotation, CaseType caseType, String loggedInUser) {
        String generatedName = createAnnotationName();

        // Ensure uniqueness of the generated name in the database
        while (annotationRepository.existsByName(generatedName)) {
            generatedName = createAnnotationName();
        }

        annotation.setName(generatedName);
        annotation.setDeleted(false);
        annotation.setLastModifiedUser(loggedInUser);
        annotation.setState(AnnotationState.ACCEPTED);
        annotation.setCreatedBy(loggedInUser);
        return annotationRepository.save(annotation);
    }

    @Transactional(readOnly = true)
    public List<Annotation> getAllAnnotationsForPatientSlide(String slideId) {
        return annotationRepository.findAllByPatientSlideIdAndIsDeleted(slideId, false);
    }

    @Transactional
    public void deleteAllAnnotationsForSlide(String slideId, String loggedInUser) {
        List<Annotation> annotations = annotationRepository.findAllByPatientSlideIdAndIsDeleted(slideId, false);
        deleteAnnotations(annotations, loggedInUser);
    }

    @Transactional
    public void deleteAllAnnotationsForSlideIdIn(List<String> slideIds, String loggedInUser) {
        List<Annotation> annotations = annotationRepository.findAllByPatientSlideIdInAndIsDeleted(slideIds, false);
        deleteAnnotations(annotations, loggedInUser);
    }

    @Transactional(readOnly = true)
    public Long getCountByPatientSlideId(String patientSlideId) {
        return annotationRepository.countAllByPatientSlideIdAndIsDeleted(patientSlideId, false);
    }

    @Transactional
    public void deleteAnnotationById(String slideId, String annotationId, String loggedInUser) {
        List<Annotation> annotations = annotationRepository.findAllByPatientSlideIdAndIdAndIsDeleted(slideId, annotationId, false);
        deleteAnnotations(annotations, loggedInUser);
    }

    @Transactional
    public void deleteAnnotations(List<Annotation> annotations, String loggedInUser) {
        annotations.forEach(annotation -> {
            annotation.setDeleted(true);
            annotation.setState(AnnotationState.REJECTED);
            annotation.setLastModifiedUser(loggedInUser);
            annotationRepository.save(annotation);
        });
    }

    private String createAnnotationName() {
        return generateRandomString() + "_" + generateUniqueHash();
    }

    private String generateUniqueHash() {
        return String.valueOf((int) (Math.random() * 1_000_000_00)); // Generates a random 8-digit number
    }

    public static String generateRandomString() {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder result = new StringBuilder(4);
        Random random = new Random();

        for (int i = 0; i < 4; i++) {
            result.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }

        return result.toString();
    }

    @Transactional(readOnly = true)
    public List<Annotation> findAll() {
        return annotationRepository.findAllByIsDeleted(false);
    }

    public List<BiologicalType> getBiologicalTypeByCategory(CaseType caseType) {
        return biologicalTypeRepository.findByCategory(caseType.toString());
    }

    @Transactional
    public Annotation updateAnnotationState(String slideId, String annotationId, AnnotationState newState, String loggedInUser) {
        Annotation annotation = annotationRepository.findByIdAndPatientSlideIdAndIsDeleted(annotationId, slideId, false)
            .orElseThrow(() -> new IllegalArgumentException("Annotation not found"));

        if (annotation.getAnnotationType() != AnnotationType.AI) {
            throw new IllegalStateException("State can only be updated for AI annotations");
        }

        annotation.setState(newState);
        annotation.setLastModifiedUser(loggedInUser);
        return annotationRepository.save(annotation);
    }

    @Transactional
    public List<Annotation> updateAnnotationStatesInBulk(String slideId, AnnotationState newState, String loggedInUser) {
        List<Annotation> annotations = annotationRepository
            .findAllByPatientSlideIdAndIsDeletedAndAnnotationType(
                slideId, false, AnnotationType.AI);

        annotations.forEach(annotation -> {
            annotation.setState(newState);
            annotation.setLastModifiedUser(loggedInUser);
        });

        return annotationRepository.saveAll(annotations);
    }
}


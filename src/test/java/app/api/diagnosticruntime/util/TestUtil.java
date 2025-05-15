package app.api.diagnosticruntime.util;

import app.api.diagnosticruntime.annotation.dto.AIAnnotationCreationData;
import app.api.diagnosticruntime.annotation.dto.AIAnnotatonCreationDTO;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.model.AnnotationType;
import app.api.diagnosticruntime.annotation.repository.AnnotationRepository;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.repository.DiseaseSpectrumRepository;
import app.api.diagnosticruntime.disease.repository.GradingRepository;
import app.api.diagnosticruntime.disease.repository.SubtypeRepository;
import app.api.diagnosticruntime.disease.service.DiseaseSpectrumService;
import app.api.diagnosticruntime.disease.service.GradingService;
import app.api.diagnosticruntime.disease.service.SubtypeService;
import app.api.diagnosticruntime.organ.model.Organ;
import app.api.diagnosticruntime.organ.repository.OrganRepository;
import app.api.diagnosticruntime.organ.service.OrganService;
import app.api.diagnosticruntime.patient.casemanagment.comment.Comment;
import app.api.diagnosticruntime.patient.casemanagment.comment.CommentRepository;
import app.api.diagnosticruntime.patient.casemanagment.comment.CommentService;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto.DiagnosisDTO;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.model.Diagnosis;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.repository.DiagnosisRepository;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.service.DiagnosisService;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseCreatedDTO;
import app.api.diagnosticruntime.patient.casemanagment.dto.CasePatientCreationDTO;
import app.api.diagnosticruntime.patient.casemanagment.feedback.model.Feedback;
import app.api.diagnosticruntime.patient.casemanagment.feedback.repository.FeedbackRepository;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.patient.casemanagment.history.repository.HistoryRepository;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.repository.CaseRepository;
import app.api.diagnosticruntime.patient.casemanagment.service.CaseService;
import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import app.api.diagnosticruntime.patient.repository.PatientRepository;
import app.api.diagnosticruntime.patient.repository.PatientSlideRepository;
import app.api.diagnosticruntime.patient.service.PatientService;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.dto.UserRegistrationDTO;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.model.UserStatus;
import app.api.diagnosticruntime.userdetails.repository.UserRepository;
import app.api.diagnosticruntime.userdetails.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.platform.commons.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TestUtil {
    @Autowired
    private CaseService caseService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private HistoryRepository historyRepository;
    @Autowired
    private PatientService patientService;
    @Autowired
    private PatientSlideRepository patientSlideRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private AnnotationRepository annotationRepository;
    @Autowired
    private AnnotationService annotationService;
    @Autowired
    private DiagnosisRepository diagnosisRepository;
    @Autowired
    private DiagnosisService diagnosisService;
    @Autowired
    private FeedbackRepository feedbackRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private PatientSlideService patientSlideService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PatientRepository patientRepository;
    @Autowired
    private CaseRepository caseRepository;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private OrganService organService;
    @Autowired
    private OrganRepository organRepository;
    @Autowired
    private DiseaseSpectrumRepository diseaseSpectrumRepository;
    @Autowired
    private GradingRepository gradingRepository;
    @Autowired
    private SubtypeRepository subtypeRepository;

    @Autowired
    private DiseaseSpectrumService diseaseSpectrumService;
    @Autowired
    private GradingService gradingService;
    @Autowired
    private SubtypeService subtypeService;


    private static final ObjectMapper objectMapper = new ObjectMapper();

    public UserInfoDTO createUser(){
        UserRegistrationDTO userRegistrationDTO = new UserRegistrationDTO();
        userRegistrationDTO.setEmail("pathologist@example.com");
        userRegistrationDTO.setRole(UserRole.PATHOLOGIST.toString());
        userRegistrationDTO.setPassword("password");

        UserInfoDTO user = userService.registerUser(userRegistrationDTO);

        userService.updateUserStatus(user.getId(), UserStatus.ACTIVE.toString());
        return user;
    }

    public void cleanUpRepositories() {
        userRepository.deleteAll();
        patientRepository.deleteAll();
        caseRepository.deleteAll();
        historyRepository.deleteAll();
        patientSlideRepository.deleteAll();
        annotationRepository.deleteAll();
        diagnosisRepository.deleteAll();
        feedbackRepository.deleteAll();
        commentRepository.deleteAll();
        organRepository.deleteAll();
        diseaseSpectrumRepository.deleteAll();
        subtypeRepository.deleteAll();
        gradingRepository.deleteAll();
    }

    public void createHistory(String caseId, CaseStatus status, String userId) {
        History history = new History();
        history.setCaseId(caseId);
        history.setPreviousStatus(null);
        history.setNewStatus(status);
        history.setActionByPathologistId(userId);
        historyRepository.save(history);
    }

    public Patient createPatient(String mrn, String gender, Integer age) {
        Patient patient = new Patient();
        patient.setMrn(mrn);
        patient.setGender(gender);
        patient.setAge(age);
        return patientRepository.save(patient);
    }

    public Patient createPatient(String mrnNumber) {
        Patient patient = new Patient();
        patient.setMrn(mrnNumber);
        patient.setAge(12);
        patient.setGender("MALE");
        return patientRepository.save(patient);
    }

    public String createManualAnnotation(String slideId, String userEmail) {
        Annotation annotation = new Annotation();
        annotation.setPatientSlideId(slideId);
        annotation.setAnnotationType(AnnotationType.MANUAL);
        annotation.setBiologicalType("MITOSIS");
        annotation.setShape("POLYGON");
        annotation.setDescription("Test manual annotation");
        annotation.setCoordinates(new HashSet<>());
        annotation.setColor("#FF0000");

        Annotation saved = annotationService.createAnnotation(annotation, CaseType.PATHOLOGY, userEmail);
        return saved.getId();
    }

    public List<String> createAIAnnotations(PatientSlide patientSlide, int count) {
        List<AIAnnotatonCreationDTO> aiDtos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AIAnnotatonCreationDTO dto = new AIAnnotatonCreationDTO();
            dto.setPatient_slide_id(patientSlide.getId());
            dto.setAnnotation_type("AI");

            AIAnnotationCreationData annotationData = new AIAnnotationCreationData();
            annotationData.setName("AI_Annotation_" + i);
            annotationData.setBiological_type("MITOSIS_METAPHASE");
            annotationData.setShape("POLYGON");
            annotationData.setDescription("AI Generated Annotation " + i);
            annotationData.setCoordinates(new HashSet<>()); // You might want to add some test coordinates

            dto.setAnnotations(Collections.singletonList(annotationData));
            aiDtos.add(dto);
        }
        aiDtos.forEach(aiAnnotatonCreationDTO -> {
            annotationService.createAIAnnotations(aiAnnotatonCreationDTO, patientSlide, false);
        });


        return annotationService.getAllAnnotationsForPatientSlide(patientSlide.getId()).stream()
                .filter(a -> a.getAnnotationType() == AnnotationType.AI)
                .map(Annotation::getId)
                .collect(Collectors.toList());
    }



    public String createCase(CaseType caseType){
        PatientDetailsDTO patientDetails = new PatientDetailsDTO();

        patientDetails.setGender("Male");

        patientDetails.setAge(35);

        patientDetails.setMrn("7895632158");

        Patient patient = patientService.createPatient(patientDetails);

        CasePatientCreationDTO caseData = new CasePatientCreationDTO();
        caseData.setPatientDetailsDTO(patientDetails);
        caseData.setCaseType(caseType);  // Set an appropriate case type

        CaseCreatedDTO caseCreatedDTO = caseService.addCase(caseData, patient.getId());
        return caseCreatedDTO.getId();
    }


    public void createInitialHistory(String caseId, String userId) {
        History initialHistory = new History();
        initialHistory.setCaseId(caseId);
        initialHistory.setPreviousStatus(null);
        initialHistory.setNewStatus(CaseStatus.NEW);
        initialHistory.setActionByPathologistId(userId);
        historyRepository.save(initialHistory);
    }

    public PatientSlide createPatientSlide(String caseId) {
        PatientSlide patientSlide = new PatientSlide();
        patientSlide.setCaseId(caseId);
        patientSlide.setSlideImagePath("path/to/image.jpg");
        patientSlide.setShowImagePath("path/to/show-image.jpg");
        patientSlide.setStatus(PatientSlideStatus.NEW);
        patientSlide.setDeleted(false);
        patientSlide.setOrganId(createOrganAndGetId("BRAIN"));
        return patientSlideRepository.save(patientSlide);
    }

    public String createOrganAndGetId(String name) {
        return organService.getOrCreateOrganId(name);
    }

    public void createAnnotationsForSlide(String slideId, String username) {
        Annotation annotation = new Annotation();
        annotation.setPatientSlideId(slideId);
        annotation.setLastModifiedUser(username);
        annotation.setName("Sample Annotation");
        annotation.setColor("red");
        annotation.setDescription("Test annotation");
        annotation.setDeleted(false);
        annotationRepository.save(annotation);
    }

    public void createCommentsForCase(String caseId, String username) {
        Comment comment = new Comment();
        comment.setCaseId(caseId);
        comment.setCreationUser(username);
        comment.setCommentText("Sample comment");
        comment.setDeleted(false);
        commentRepository.save(comment);
    }

    public void createDiagnosis(String caseId, String userId) {
        Diagnosis diagnosis = new Diagnosis();
        diagnosis.setCaseId(caseId);
        diagnosis.setUserId(userId);
        diagnosis.setDiagnosis("Test Diagnosis");
        diagnosis.setDeleted(false);
        diagnosisRepository.save(diagnosis);
    }

    public void createFeedback(String caseId, String userId) {
        Feedback feedback = new Feedback();
        feedback.setCaseId(caseId);
        feedback.setUserId(userId);
        feedback.setDifficultyLevel(2);
        feedback.setFeedback("Test feedback");
        feedback.setDeleted(false);
        feedbackRepository.save(feedback);
    }

    public void completeAllSlides(String caseId) {
        List<PatientSlide> slides = patientSlideService.getAllByCaseId(caseId);
        for (PatientSlide slide : slides) {
            slide.setStatus(PatientSlideStatus.COMPLETED);
            patientSlideRepository.save(slide);
        }
    }

    public Optional<Case> getCaseById(String id){
        return caseService.getCaseById(id);
    }

    public List<Annotation> getAllAnnotationsForPatientSlide(String id){
        return annotationService.getAllAnnotationsForPatientSlide(id);
    }

    public List<PatientSlide> getAllPatientSlidesByCaseId(String caseId) {
        return patientSlideRepository.findAllByCaseIdAndIsDeleted(caseId, false);
    }

    public List<Comment> getAllCommentsByCaseId(String caseId) {
        return commentService.getAllCommentsByCaseId(caseId);
    }

    public List<DiagnosisDTO> getAllDiagnosisByCaseId(String caseId) {
        return diagnosisService.getDiagnosesByCaseId(caseId);
    }

    public Case createCaseWithPatient(CaseType caseType, String patientId) {
        Case caseToCreate = new Case();
        caseToCreate.setCaseType(caseType);
        caseToCreate.setName(caseType.name() + "-" + System.currentTimeMillis());
        caseToCreate.setStatus(CaseStatus.NEW);
        caseToCreate.setDate(LocalDateTime.now());
        caseToCreate.setDeleted(false);
        caseToCreate.setPatientId(patientId);
        return caseRepository.save(caseToCreate);
    }

    public Case createCaseWithPatientAndDate(CaseType caseType, String patientId, LocalDateTime date) {
        Case caseToCreate = new Case();
        caseToCreate.setCaseType(caseType);
        caseToCreate.setName(caseType.name() + "-" + System.currentTimeMillis());
        caseToCreate.setStatus(CaseStatus.NEW);
        caseToCreate.setDate(date);
        caseToCreate.setDeleted(false);
        caseToCreate.setPatientId(patientId);
        return caseRepository.save(caseToCreate);
    }

    public UserInfoDTO createUser(String email, String firstName, String lastName, UserRole role) {
        UserRegistrationDTO registrationDTO = new UserRegistrationDTO();
        registrationDTO.setEmail(email);
        registrationDTO.setPassword("password123");
        registrationDTO.setFullName(firstName + " " + lastName);
        registrationDTO.setRole(role.toString());
        UserInfoDTO user = userService.registerUser(registrationDTO);
        userService.updateUserStatus(user.getId(), UserStatus.ACTIVE.toString());
        return user;
    }

    public static String extractJsonField(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            return node.get(fieldName).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract field from JSON", e);
        }
    }

    public boolean doesCaseExist(String caseId) {
        return mongoTemplate.exists(Query.query(Criteria.where("_id").is(caseId)), "cases");
    }

    public boolean doesPatientExistByMrn(String mrn) {
        return mongoTemplate.exists(Query.query(Criteria.where("mrn").is(mrn)), "patients");
    }

    public boolean doesPatientExistByPraidId(Long praidId) {
        return mongoTemplate.exists(Query.query(Criteria.where("praid").is(praidId)), "patients");
    }

    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert to JSON", e);
        }
    }

    public void updateUserStatus(String id, UserStatus userStatus) {
        userService.updateUserStatus(id, userStatus.toString());
    }

    public PatientSlide getPatientSlideBySlideId(String slideId) {
        return patientSlideRepository.getPatientSlideByIdAndIsDeleted(slideId, false).orElse(null);
    }

    public DiseaseSpectrumDTO createDiseaseSpectrum(DiseaseSpectrumDTO dto) {
        if(StringUtils.isNotBlank(dto.getOrganName())) {
            Organ organ = organService.getOrCreateOrgan(dto.getOrganName());
            dto.setOrganName(organ.getName());
        }
        return diseaseSpectrumService.createOrGetDiseaseSpectrum(dto);
    }

    public GradingDTO createGrading(GradingDTO dto) {
        return gradingService.createGrading(dto);
    }

    public SubtypeDTO createSubtype(SubtypeDTO dto) {
        return subtypeService.createOrGetSubtype(dto);
    }

    public void createPatientSlideWithOrgan(String caseId, String organName) {
        String organId = organService.getOrCreateOrganId(organName);
        PatientSlide slide = new PatientSlide();
        slide.setCaseId(caseId);
        slide.setOrganId(organId);
        slide.setDeleted(false);
        patientSlideRepository.save(slide);
    }
}
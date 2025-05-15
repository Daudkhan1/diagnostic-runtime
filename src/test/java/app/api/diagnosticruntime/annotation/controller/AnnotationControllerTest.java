package app.api.diagnosticruntime.annotation.controller;

import app.api.diagnosticruntime.annotation.dto.AnnotationDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationDetailsDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationUpdateDTO;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.model.AnnotationState;
import app.api.diagnosticruntime.annotation.model.AnnotationType;
import app.api.diagnosticruntime.annotation.repository.AnnotationRepository;
import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.repository.DiseaseSpectrumRepository;
import app.api.diagnosticruntime.disease.repository.GradingRepository;
import app.api.diagnosticruntime.disease.repository.SubtypeRepository;
import app.api.diagnosticruntime.organ.service.OrganService;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.util.TestUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none")
public class AnnotationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DiseaseSpectrumRepository diseaseSpectrumRepository;

    @Autowired
    private SubtypeRepository subtypeRepository;

    @Autowired
    private GradingRepository gradingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganService organService;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private AnnotationRepository annotationRepository;

    @BeforeAll
    static void startContainer() {
        MongoDBTestContainer.start();
    }

    @AfterAll
    static void stopContainer() {
        MongoDBTestContainer.stop();
    }

    @BeforeEach
    void setUp() {
        testUtil.cleanUpRepositories();
    }

    @Test
    @Order(1)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldCreateAndUpdateManualAnnotation() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        String slideId = testUtil.createPatientSlide(caseId).getId();

        // 2. Create manual annotation
        String annotationJson = """
        {
            "patientSlideId": "%s",
            "annotationType": "MANUAL",
            "biologicalType": "MITOSIS",
            "shape": "POLYGON",
            "caseType":"PATHOLOGY",
            "description": "Test description",
            "coordinates": [],
            "color": "#FF0000"
        }
    """.formatted(slideId);

        // Perform create request and extract the ID from response
        String annotationId = mockMvc.perform(post("/api/slide/annotation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(annotationJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(AnnotationState.ACCEPTED.toString()))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .split("\"id\":\"")[1]
                .split("\"")[0];

        // 3. Update annotation
        String updateJson = """
        {
            "description": "Updated description"
        }
    """;

        mockMvc.perform(put("/api/slide/" + slideId + "/annotations/" + annotationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)
                        .param("caseType", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated description"));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldHandleAIAnnotationStates() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        // 2. Create AI annotations
        List<String> aiAnnotationIds = testUtil.createAIAnnotations(patientSlide, 3);

        // 3. Verify initial state is NEW
        mockMvc.perform(get("/api/slide/" + patientSlide.getId() + "/annotation/" + aiAnnotationIds.get(0)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(AnnotationState.NEW.toString()));

        // 4. Accept single annotation
        mockMvc.perform(put("/api/slide/" + patientSlide.getId() + "/annotations/" + aiAnnotationIds.get(0) + "/state")
                        .param("state", AnnotationState.ACCEPTED.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(AnnotationState.ACCEPTED.toString()));

        // 5. Bulk accept remaining annotations
        mockMvc.perform(put("/api/slide/" + patientSlide.getId() + "/annotations/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("state", AnnotationState.ACCEPTED.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].state").value(AnnotationState.ACCEPTED.toString()))
                .andExpect(jsonPath("$[1].state").value(AnnotationState.ACCEPTED.toString()));

        // 6. Delete annotation (should set state to REJECTED)
        mockMvc.perform(delete("/api/slide/" + patientSlide.getId() + "/annotation/" + aiAnnotationIds.get(0)))
                .andExpect(status().isNoContent());

        Optional<Annotation> annotation = annotationRepository.findById(aiAnnotationIds.get(0));
        assertEquals("Should be rejected", AnnotationState.REJECTED, annotation.get().getState());
        assertEquals("Should be deleted", true, annotation.get().isDeleted());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldPreventStateChangeForManualAnnotations() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        String slideId = testUtil.createPatientSlide(caseId).getId();

        // 2. Create manual annotation
        String annotationId = testUtil.createManualAnnotation(slideId, user.getEmail());

        // 3. Attempt to change state of manual annotation
        mockMvc.perform(put("/api/slide/" + slideId + "/annotations/" + annotationId + "/state")
                        .param("state", AnnotationState.NEW.toString()))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetAllAnnotationsForSlide() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        // 2. Create both manual and AI annotations
        testUtil.createManualAnnotation(patientSlide.getId(), user.getEmail());
        List<String> aiAnnotationIds = testUtil.createAIAnnotations(patientSlide, 2);

        // 3. Get and verify annotations
        MvcResult result = mockMvc.perform(get("/api/slide/" + patientSlide.getId() + "/annotation"))
                .andExpect(status().isOk())
                .andReturn();

        // Convert response to list of AnnotationDTO
        ObjectMapper objectMapper = new ObjectMapper();
        List<AnnotationDTO> annotations = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<List<AnnotationDTO>>() {}
        );

        // Verify total count
        assertEquals("Should be 3",3, annotations.size());

        // Verify manual annotation
        List<AnnotationDTO> manualAnnotations = annotations.stream()
                .filter(a -> a.getAnnotationType() == AnnotationType.MANUAL)
                .collect(Collectors.toList());
        assertEquals("Should be one",1, manualAnnotations.size());
        assertEquals("Should be accepted", AnnotationState.ACCEPTED, manualAnnotations.get(0).getState());

        // Verify AI annotations
        List<AnnotationDTO> aiAnnotations = annotations.stream()
                .filter(a -> a.getAnnotationType() == AnnotationType.AI)
                .collect(Collectors.toList());
        assertEquals("Should be size 2",2, aiAnnotations.size());
        assertTrue(aiAnnotations.stream().allMatch(a -> a.getState() == AnnotationState.NEW));
    }

    @Test
    @Order(5)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldCreateAnnotationWithDiseaseEntities() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        String slideId = testUtil.createPatientSlide(caseId).getId();
        organService.getOrCreateOrganName("LIVER");


        // 2. Create annotation with disease entities
        DiseaseSpectrumDTO diseaseSpectrumDTO = new DiseaseSpectrumDTO();
        diseaseSpectrumDTO.setName("Spectrum1");
        diseaseSpectrumDTO.setOrganName("LIVER");

        SubtypeDTO subtypeDTO = new SubtypeDTO();
        subtypeDTO.setName("Subtype1");
        subtypeDTO.setOrganName("LIVER");

        GradingDTO gradingDTO = new GradingDTO();
        gradingDTO.setName("Grade1");
        gradingDTO.setDiseaseSpectrumName("Spectrum1");
        gradingDTO.setOrganName("LIVER");

        AnnotationDTO annotationDTO = new AnnotationDTO();
        annotationDTO.setPatientSlideId(slideId);
        annotationDTO.setAnnotationType(AnnotationType.MANUAL);
        annotationDTO.setBiologicalType("MITOSIS");
        annotationDTO.setShape("POLYGON");
        annotationDTO.setCaseType(CaseType.PATHOLOGY);
        annotationDTO.setDescription("Test description");
        annotationDTO.setColor("#FF0000");
        annotationDTO.setDiseaseSpectrum(diseaseSpectrumDTO);
        annotationDTO.setSubtype(subtypeDTO);
        annotationDTO.setGrading(gradingDTO);

        // Perform create request
        MvcResult result = mockMvc.perform(post("/api/slide/annotation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(annotationDTO)))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        AnnotationDTO createdAnnotation = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                AnnotationDTO.class
        );

        // Verify disease entities were created
        assertNotNull(createdAnnotation.getDiseaseSpectrum().getId());
        assertNotNull(createdAnnotation.getSubtype().getId());
        assertNotNull(createdAnnotation.getGrading().getId());

        // Verify entities exist in database
        assertTrue(diseaseSpectrumRepository.findById(createdAnnotation.getDiseaseSpectrum().getId()).isPresent());
        assertTrue(subtypeRepository.findById(createdAnnotation.getSubtype().getId()).isPresent());
        assertTrue(gradingRepository.findById(createdAnnotation.getGrading().getId()).isPresent());
    }

    @Test
    @Order(6)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldUpdateAnnotationWithNewDiseaseEntities() throws Exception {
        // 1. Create initial annotation
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        String slideId = testUtil.createPatientSlide(caseId).getId();
        String annotationId = testUtil.createManualAnnotation(slideId, user.getEmail());

        organService.getOrCreateOrganName("KIDNEY");

        // 2. Create update DTO with new disease entities
        DiseaseSpectrumDTO newDiseaseSpectrumDTO = new DiseaseSpectrumDTO();
        newDiseaseSpectrumDTO.setName("UpdatedSpectrum");
        newDiseaseSpectrumDTO.setOrganName("KIDNEY");

        SubtypeDTO newSubtypeDTO = new SubtypeDTO();
        newSubtypeDTO.setName("UpdatedSubtype");
        newSubtypeDTO.setOrganName("KIDNEY");

        GradingDTO newGradingDTO = new GradingDTO();
        newGradingDTO.setName("UpdatedGrade");
        newGradingDTO.setDiseaseSpectrumName("UpdatedSpectrum");
        newGradingDTO.setOrganName("KIDNEY");

        AnnotationUpdateDTO updateDTO = new AnnotationUpdateDTO();
        updateDTO.setBiologicalType("UPDATED_TYPE");
        updateDTO.setDescription("Updated description");
        updateDTO.setDiseaseSpectrum(newDiseaseSpectrumDTO);
        updateDTO.setSubtype(newSubtypeDTO);
        updateDTO.setGrading(newGradingDTO);

        // Perform update request
        MvcResult result = mockMvc.perform(put("/api/slide/" + slideId + "/annotations/" + annotationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO))
                        .param("caseType", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        Annotation updatedAnnotation = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                Annotation.class
        );

        // Verify disease entities were created and linked
        assertNotNull(updatedAnnotation.getDiseaseSpectrumId());
        assertNotNull(updatedAnnotation.getSubtypeId());
        assertNotNull(updatedAnnotation.getGradingId());

        // Verify entities exist in database
        assertTrue(diseaseSpectrumRepository.findById(updatedAnnotation.getDiseaseSpectrumId()).isPresent());
        assertTrue(subtypeRepository.findById(updatedAnnotation.getSubtypeId()).isPresent());
        assertTrue(gradingRepository.findById(updatedAnnotation.getGradingId()).isPresent());

        // Verify the content of entities
        assertEquals("","UpdatedSpectrum",
                diseaseSpectrumRepository.findById(updatedAnnotation.getDiseaseSpectrumId()).get().getName());
        assertEquals("","UpdatedSubtype",
                subtypeRepository.findById(updatedAnnotation.getSubtypeId()).get().getName());
        assertEquals("","UpdatedGrade",
                gradingRepository.findById(updatedAnnotation.getGradingId()).get().getName());
    }

    @Test
    @Order(7)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetCorrectAnnotationDetails() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        String slideId = testUtil.createPatientSlide(caseId).getId();
        organService.getOrCreateOrganName("LIVER");

        // 2. Create disease entities
        DiseaseSpectrumDTO diseaseSpectrumDTO = new DiseaseSpectrumDTO();
        diseaseSpectrumDTO.setName("TestSpectrum");
        diseaseSpectrumDTO.setOrganName("LIVER");

        SubtypeDTO subtypeDTO = new SubtypeDTO();
        subtypeDTO.setName("TestSubtype");
        subtypeDTO.setOrganName("LIVER");

        GradingDTO gradingDTO = new GradingDTO();
        gradingDTO.setName("TestGrade");
        gradingDTO.setDiseaseSpectrumName("TestSpectrum");
        gradingDTO.setOrganName("LIVER");

        // 3. Create annotation with all fields
        AnnotationDTO annotationDTO = new AnnotationDTO();
        annotationDTO.setPatientSlideId(slideId);
        annotationDTO.setAnnotationType(AnnotationType.MANUAL);
        annotationDTO.setBiologicalType("MITOSIS");
        annotationDTO.setShape("POLYGON");
        annotationDTO.setCaseType(CaseType.PATHOLOGY);
        annotationDTO.setDescription("Test description");
        annotationDTO.setColor("#FF0000");
        annotationDTO.setDiseaseSpectrum(diseaseSpectrumDTO);
        annotationDTO.setSubtype(subtypeDTO);
        annotationDTO.setGrading(gradingDTO);

        // 4. Create annotation and get its ID
        MvcResult createResult = mockMvc.perform(post("/api/slide/annotation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(annotationDTO)))
                .andExpect(status().isOk())
                .andReturn();

        AnnotationDTO createdAnnotation = objectMapper.readValue(
                createResult.getResponse().getContentAsString(),
                AnnotationDTO.class
        );

        // 5. Get annotation details
        MvcResult detailsResult = mockMvc.perform(get("/api/slide/" + slideId + "/annotation/" + createdAnnotation.getId() + "/details")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // 6. Parse and verify response
        AnnotationDetailsDTO details = objectMapper.readValue(
                detailsResult.getResponse().getContentAsString(),
                AnnotationDetailsDTO.class
        );

        // Verify all fields
        assertEquals("Must be equal", createdAnnotation.getId(), details.getId());
        assertEquals("Must be equal","Test description", details.getDescription());
        assertEquals("Must be equal","MITOSIS", details.getBiologicalType());
        assertEquals("Must be equal","POLYGON", details.getShape());
        assertEquals("Must be equal","MANUAL", details.getAnnotationType());
        assertEquals("Must be equal","TestSpectrum", details.getDiseaseSpectrum());
        assertEquals("Must be equal","TestSubtype", details.getSubType());
        assertEquals("Must be equal","TestGrade", details.getGrading());
        assertEquals("Must be equal","PATHOLOGIST", details.getRole());
        assertEquals("Must be equal",user.getFullName(), details.getAnnotatedBy());
    }
}
package app.api.diagnosticruntime.patient.controller;

import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.organ.model.Organ;
import app.api.diagnosticruntime.organ.service.OrganService;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.dto.PatientSlideAddDTO;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import app.api.diagnosticruntime.patient.repository.PatientSlideRepository;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.util.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.testbase.replace=none")
public class PatientSlideControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private OrganService organService;

    @Autowired
    private PatientSlideRepository patientSlideRepository;

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
    void shouldAddPatientSlideSuccessfully() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 2. Create patient slide add DTO
        PatientSlideAddDTO slideDTO = new PatientSlideAddDTO();
        slideDTO.setCaseId(caseId);
        slideDTO.setOrgan("Brain");
        slideDTO.setSlideImagePath("/path/to/slide/image.tiff");
        slideDTO.setShowImagePath("/path/to/show/image.tiff");
        slideDTO.setMicroMeterPerPixel("0.25");
        slideDTO.setStatus(PatientSlideStatus.NEW);

        // 3. Perform the request to add a slide
        mockMvc.perform(post("/api/patient/slide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(slideDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.caseId").value(caseId))
                .andExpect(jsonPath("$.organ").value("BRAIN"))
                .andExpect(jsonPath("$.slideImagePath").value("/path/to/slide/image.tiff"))
                .andExpect(jsonPath("$.showImagePath").value("/path/to/show/image.tiff"))
                .andExpect(jsonPath("$.microMeterPerPixel").value("0.25"))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailToAddPatientSlideWithInvalidCase() throws Exception {
        // 1. Get initial count of slides
        long initialSlideCount = patientSlideRepository.count();

        // 2. Attempt to create slide with invalid case
        PatientSlideAddDTO slideDTO = new PatientSlideAddDTO();
        slideDTO.setCaseId("invalid_case_id");
        slideDTO.setOrgan("Brain");
        slideDTO.setSlideImagePath("/path/to/slide/image.tiff");
        slideDTO.setStatus(PatientSlideStatus.NEW);

        // 3. Perform request and expect error
        mockMvc.perform(post("/api/patient/slide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(slideDTO)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false));

        // 4. Verify no new slide was created
        long finalSlideCount = patientSlideRepository.count();
        Assertions.assertEquals(initialSlideCount, finalSlideCount, 
            "No new slide should be created when case ID is invalid");

        // 5. Verify no slide exists with this invalid case ID
        List<PatientSlide> slidesWithInvalidCase = patientSlideRepository.findAllByCaseIdAndIsDeleted("invalid_case_id", false);
        Assertions.assertTrue(slidesWithInvalidCase.isEmpty(), 
            "No slides should exist with invalid case ID");
    }

    @Test
    @Order(3)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailToAddPatientSlideWithMissingRequiredFields() throws Exception {
        PatientSlideAddDTO slideDTO = new PatientSlideAddDTO();
        // Only set case ID, missing other required fields
        slideDTO.setCaseId("some_case_id");

        mockMvc.perform(post("/api/patient/slide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(slideDTO)))
                .andExpect(status().is5xxServerError());
    }


    @Test
    @Order(4)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldCreateAndReuseOrgan() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 2. Verify organ doesn't exist before creating slide
        List<String> organNamesBefore = organService.getAllOrganNames();
        Assertions.assertFalse(organNamesBefore.contains("Liver"), "Organ should not exist before test");

        // 3. Create first slide with new organ
        PatientSlideAddDTO firstSlideDTO = new PatientSlideAddDTO();
        firstSlideDTO.setCaseId(caseId);
        firstSlideDTO.setOrgan("Liver");  // New organ
        firstSlideDTO.setSlideImagePath("/path/to/first/image.tiff");
        firstSlideDTO.setShowImagePath("/path/to/first/show/image.tiff");
        firstSlideDTO.setMicroMeterPerPixel("0.25");
        firstSlideDTO.setStatus(PatientSlideStatus.NEW);

        // 4. Add first slide and verify organ was created
        mockMvc.perform(post("/api/patient/slide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(firstSlideDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organ").value("LIVER"));

        // 5. Verify organ exists in database after first slide
        List<String> organNamesAfterFirst = organService.getAllOrganNames();
        Assertions.assertTrue(organNamesAfterFirst.contains("LIVER"), "Organ should exist after creating first slide");

        // Get the organ ID for verification
        String organId = organService.getOrCreateOrganName("LIVER");
        Assertions.assertNotNull(organId, "Organ ID should not be null");

        // 6. Create second slide with same organ
        PatientSlideAddDTO secondSlideDTO = new PatientSlideAddDTO();
        secondSlideDTO.setCaseId(caseId);
        secondSlideDTO.setOrgan("LIVER");  // Same organ name
        secondSlideDTO.setSlideImagePath("/path/to/second/image.tiff");
        secondSlideDTO.setShowImagePath("/path/to/second/show/image.tiff");
        secondSlideDTO.setMicroMeterPerPixel("0.25");
        secondSlideDTO.setStatus(PatientSlideStatus.NEW);

        // 7. Add second slide and verify it uses the same organ
        mockMvc.perform(post("/api/patient/slide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(secondSlideDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organ").value("LIVER"));

        // 8. Verify no new organ was created (count should remain the same)
        List<String> organNamesAfterSecond = organService.getAllOrganNames();
        Assertions.assertEquals(
            organNamesAfterFirst.size(),
            organNamesAfterSecond.size(),
            "No new organ should be created for the second slide"
        );

        // 9. Verify both slides reference the same organ
        List<Organ> organs = organService.getAllOrgans();
        long organCount = organs.stream()
                .filter(organ -> organ.getName().equals("LIVER"))
                .count();
        Assertions.assertEquals(1, organCount, "There should be exactly one organ with name 'LIVER'");
    }
} 
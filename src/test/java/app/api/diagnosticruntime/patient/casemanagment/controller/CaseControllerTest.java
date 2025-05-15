package app.api.diagnosticruntime.patient.casemanagment.controller;

import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.patient.casemanagment.dto.CasePatientCreationDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.service.HistoryService;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.dto.PatientDetailsDTO;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.service.UserService;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none")
public class CaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private HistoryService historyService;

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
    void shouldSearchCasesByStatus() throws Exception {
        // 1. Create user and cases
        UserInfoDTO user = testUtil.createUser();
        UserInfoDTO user2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);
        User loggedInUser2 = userService.getUserByUsername(user2.getEmail());
        String caseId1 = testUtil.createCase(CaseType.PATHOLOGY);
        String caseId2 = testUtil.createCase(CaseType.PATHOLOGY);
        String caseId3 = testUtil.createCase(CaseType.RADIOLOGY);

        // 2. Create history entries
        testUtil.createInitialHistory(caseId1, user.getId());
        testUtil.createInitialHistory(caseId2, user.getId());
        testUtil.createInitialHistory(caseId3, user.getId());

        // 3. Assign one case
        User loggedInUser = userService.getUserByUsername(user.getEmail());
        historyService.assignCase(caseId1, loggedInUser);

        // 4. Search for IN_PROGRESS cases
        mockMvc.perform(get("/api/case")
                        .param("status", CaseStatus.IN_PROGRESS.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));;


        // 5. Create slides and annotations for case1
        PatientSlide slide = testUtil.createPatientSlide(caseId1);
        testUtil.createAnnotationsForSlide(slide.getId(), user.getEmail());
        testUtil.completeAllSlides(caseId1);

        // 6. Set diagnosis and feedback
        testUtil.createDiagnosis(caseId1, user.getId());
        testUtil.createFeedback(caseId1, user.getId());

        // 7. Transfer case
        historyService.transferCase(caseId1, loggedInUser, user2.getId());

        // 8. Search for NEW cases
        mockMvc.perform(get("/api/case")
                        .param("status", CaseStatus.NEW.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId2))
                .andExpect(jsonPath("$.content[0].incoming").value(false));

        // 9. Search for IN_PROGRESS cases
        mockMvc.perform(get("/api/case")
                        .param("status", CaseStatus.IN_PROGRESS.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // 10. Search for REFERRED cases
        mockMvc.perform(get("/api/case")
                        .param("status", CaseStatus.REFERRED.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1))
                .andExpect(jsonPath("$.content[0].incoming").value(false));

        // 10. Search for INCOMING cases
        mockMvc.perform(get("/api/case")
                        .with(user(user.getEmail()))
                        .param("status", CaseStatus.IN_COMING.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        mockMvc.perform(get("/api/case")
                        .with(user(user2.getEmail()))
                        .param("status", CaseStatus.IN_COMING.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1))
                .andExpect(jsonPath("$.content[0].incoming").value(true));

        // 12. Search for COMPLETE cases
        mockMvc.perform(get("/api/case")
                        .param("status", CaseStatus.COMPLETE.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // 13. Complete the case
        testUtil.createDiagnosis(caseId1, user2.getId());
        testUtil.createFeedback(caseId1, user2.getId());
        testUtil.completeAllSlides(caseId1);
        historyService.completeCase(caseId1, loggedInUser2);

        // 14. Search for COMPLETE cases again
        mockMvc.perform(get("/api/case")
                        .with(user(user2.getEmail()))
                        .param("status", CaseStatus.COMPLETE.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));

        mockMvc.perform(get("/api/case")
                        .with(user(user.getEmail()))
                        .param("status", CaseStatus.COMPLETE.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldSearchCasesByLoggedInUser() throws Exception {
        // 1. Create two users
        UserInfoDTO user1 = testUtil.createUser("pathologist1@example.com", "John", "Doe", UserRole.PATHOLOGIST);
        UserInfoDTO user2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);
        UserInfoDTO user3 = testUtil.createUser("pathologist3@example.com", "Alex", "Rider", UserRole.PATHOLOGIST);

        // 2. Create cases for both users
        String caseId1 = testUtil.createCase(CaseType.PATHOLOGY);
        String caseId2 = testUtil.createCase(CaseType.PATHOLOGY);
        String caseId3 = testUtil.createCase(CaseType.PATHOLOGY);

        // 3. Create history entries
        testUtil.createInitialHistory(caseId1, user1.getId());
        testUtil.createInitialHistory(caseId2, user1.getId());
        testUtil.createInitialHistory(caseId3, user2.getId());

        // 4. Assign cases to different users
        User loggedInUser1 = userService.getUserByUsername(user1.getEmail());
        User loggedInUser2 = userService.getUserByUsername(user2.getEmail());

        historyService.assignCase(caseId1, loggedInUser1);
        historyService.assignCase(caseId3, loggedInUser2);

        // 5. Search for IN_PROGRESS cases as user1
        mockMvc.perform(get("/api/case")
                        .with(user(user1.getEmail()))
                        .param("status", CaseStatus.IN_PROGRESS.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));

        // 6. Search for NEW cases as user1
        mockMvc.perform(get("/api/case")
                        .with(user(user1.getEmail()))
                        .param("status", CaseStatus.NEW.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId2));

        // 7. Switch to user2 and verify they see different results
        mockMvc.perform(get("/api/case")
                        .with(user(user2.getEmail()))
                        .param("status", CaseStatus.IN_PROGRESS.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId3));

        // 8. Refer case1 for user1 to user2
        testUtil.createDiagnosis(caseId1, user1.getId());
        testUtil.createFeedback(caseId1, user1.getId());
        PatientSlide slide = testUtil.createPatientSlide(caseId1);
        testUtil.createAnnotationsForSlide(slide.getId(), user1.getEmail());
        testUtil.completeAllSlides(caseId1);
        historyService.transferCase(caseId1, loggedInUser1, user2.getId());

        // 9. Verify user1 sees the referred case
        mockMvc.perform(get("/api/case")
                        .with(user(user1.getEmail()))
                        .param("status", CaseStatus.REFERRED.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));

        // 10. Verify user2 sees the incoming case
        mockMvc.perform(get("/api/case")
                        .with(user(user2.getEmail()))
                        .param("status", CaseStatus.IN_COMING.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));


        testUtil.completeAllSlides(caseId1);
        testUtil.createDiagnosis(caseId1, user2.getId());
        testUtil.createFeedback(caseId1, user2.getId());
        historyService.completeCase(caseId1, loggedInUser2);

        // 9. Verify user1 sees the completed case
        mockMvc.perform(get("/api/case")
                        .with(user(user1.getEmail()))
                        .param("status", CaseStatus.COMPLETE.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));

        // 10. Verify user2 sees the completed case
        mockMvc.perform(get("/api/case")
                        .with(user(user2.getEmail()))
                        .param("status", CaseStatus.COMPLETE.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId1));

        // 11. Verify user3 doesn't see the completed case
        mockMvc.perform(get("/api/case")
                        .with(user(user3.getEmail()))
                        .param("status", CaseStatus.COMPLETE.toString())
                        .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }


    @Test
    @Order(3)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldSearchCasesByMRN() throws Exception {
        // 1. Create user and patient
        UserInfoDTO user = testUtil.createUser();
        Patient patient = testUtil.createPatient("123456789", "MALE", 30);

        // 2. Create case with patient
        Case caseWithPatient = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient.getId());
        testUtil.createInitialHistory(caseWithPatient.getId(), user.getId());

        // 3. Search by mrn number
        mockMvc.perform(get("/api/case")
                        .param("mrn", "123456789"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseWithPatient.getId()));

        // 4. Search with non-existent mrn number
        mockMvc.perform(get("/api/case")
                        .param("mrn", "99999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Order(4)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetCasesById() throws Exception {
        // 1. Create user and patient
        UserInfoDTO user = testUtil.createUser();
        Patient patient = testUtil.createPatient("123456789", "MALE", 30);

        // 2. Create case with patient
        Case caseWithPatient = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient.getId());
        testUtil.createInitialHistory(caseWithPatient.getId(), user.getId());

        // 3. Search by ID
        mockMvc.perform(get("/api/case/" + caseWithPatient.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(caseWithPatient.getId()))
                .andExpect(jsonPath("$.caseName").value(caseWithPatient.getName()))
                .andExpect(jsonPath("$.status").value(caseWithPatient.getStatus().toString()))
                .andExpect(jsonPath("$.caseType").value(caseWithPatient.getCaseType().toString()))
                .andExpect(jsonPath("$.patientDetailsDTO.id").value(patient.getId()))
                .andExpect(jsonPath("$.patientDetailsDTO.mrn").value(patient.getDecryptedMrn()))
                .andExpect(jsonPath("$.slides").isArray())
                .andExpect(jsonPath("$.slides.length()").value(0))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(0))
                .andExpect(jsonPath("$.incoming").value(false));


    }

    @Test
    @Order(5)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldCreateCaseSuccessfully() throws Exception {

        UserInfoDTO user = testUtil.createUser();
        // 1. Prepare request body
        String mrn = "MRN12345";
        PatientDetailsDTO patientDetails = new PatientDetailsDTO();
        patientDetails.setGender("Male");
        patientDetails.setAge(30);
        patientDetails.setMrn(mrn);

        CasePatientCreationDTO caseRequest = new CasePatientCreationDTO();
        caseRequest.setPatientDetailsDTO(patientDetails);
        caseRequest.setCaseType(CaseType.PATHOLOGY);

        // 2. Perform the request to add a case
        String caseResponse = mockMvc.perform(post("/api/case/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(caseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString(); // Ensure case ID is returned

        // Extract case ID
        String caseId = testUtil.extractJsonField(caseResponse, "id");

        // 3. Verify case exists in the database
        Assertions.assertTrue(testUtil.doesCaseExist(caseId), "Case should exist in the database");

        // 4. Verify patient exists in the database
        mockMvc.perform(get("/api/case")
                        .with(user(user.getEmail()))
                        .param("mrn", mrn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(caseId));
    }

    @Test
    @Order(6)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldCreateCaseSuccessfullyWithPraidId() throws Exception {

        UserInfoDTO user = testUtil.createUser();
        // 1. Prepare request body
        String mrn = "MRN12345";
        PatientDetailsDTO patientDetails = new PatientDetailsDTO();
        patientDetails.setGender("Male");
        patientDetails.setAge(30);
        patientDetails.setMrn(mrn);

        CasePatientCreationDTO caseRequest = new CasePatientCreationDTO();
        caseRequest.setPatientDetailsDTO(patientDetails);
        caseRequest.setCaseType(CaseType.PATHOLOGY);

        // 2. Perform the request to add a case
        String caseResponse = mockMvc.perform(post("/api/case/patient")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(testUtil.toJson(caseRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andReturn().getResponse().getContentAsString(); // Ensure case ID is returned

        Patient patient = testUtil.createPatient("MRN8596", "MALE", 58);
        Assertions.assertTrue(patient.getPraidId().equals(2L));
        // Extract case ID
        String caseId = testUtil.extractJsonField(caseResponse, "id");

        // 3. Verify case exists in the database
        Assertions.assertTrue(testUtil.doesCaseExist(caseId), "Case should exist in the database");
        Assertions.assertTrue(testUtil.doesPatientExistByPraidId(1L), "Patient should exist in the database");

        // 4. Verify patient exists in the database
        mockMvc.perform(get("/api/case")
                        .with(user(user.getEmail()))
                        .param("praidId", "PRAID-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].patientDetailsDTO.praidId").value("PRAID-1"))
                .andExpect(jsonPath("$.content[0].id").value(caseId));
    }

    @Test
    @Order(7)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldSearchCasesByFilters() throws Exception {
        // 1. Create user
        UserInfoDTO user = testUtil.createUser();

        // 2. Create patients with different attributes
        Patient youngMalePatient = testUtil.createPatient("MRN123", "Male", 25);
        Patient oldMalePatient = testUtil.createPatient("MRN456", "Male", 75);
        Patient youngFemalePatient = testUtil.createPatient("MRN789", "Female", 30);

        // 3. Create cases with different organs and dates
        Case heartCase = testUtil.createCaseWithPatientAndDate(CaseType.PATHOLOGY, youngMalePatient.getId(), LocalDateTime.now().minusDays(5));
        testUtil.createPatientSlideWithOrgan(heartCase.getId(), "HEART");
        testUtil.createInitialHistory(heartCase.getId(), user.getId());

        Case liverCase = testUtil.createCaseWithPatientAndDate(CaseType.PATHOLOGY, oldMalePatient.getId(), LocalDateTime.now().minusDays(15));
        testUtil.createPatientSlideWithOrgan(liverCase.getId(), "LIVER");
        testUtil.createInitialHistory(liverCase.getId(), user.getId());

        Case brainCase = testUtil.createCaseWithPatientAndDate(CaseType.PATHOLOGY, youngFemalePatient.getId(), LocalDateTime.now().minusDays(1));
        testUtil.createPatientSlideWithOrgan(brainCase.getId(), "BRAIN");
        testUtil.createInitialHistory(brainCase.getId(), user.getId());

        // 4. Test organ filter
        mockMvc.perform(get("/api/case")
                        .param("organ", "HEART"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(heartCase.getId()));

        // 5. Test gender filter
        mockMvc.perform(get("/api/case")
                        .param("gender", "Female"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(brainCase.getId()));

        // 6. Test age range filter
        mockMvc.perform(get("/api/case")
                        .param("minAge", "20")
                        .param("maxAge", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '" + heartCase.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + brainCase.getId() + "')]").exists());

        // 7. Test combined filters
        mockMvc.perform(get("/api/case")
                        .param("gender", "Male")
                        .param("minAge", "60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(liverCase.getId()));

        // 8. Test non-existent organ
        mockMvc.perform(get("/api/case")
                        .param("organ", "NONEXISTENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // 9. Test non-matching combined filters
        mockMvc.perform(get("/api/case")
                        .param("organ", "HEART")
                        .param("gender", "Female"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));

        // Test date range filter
        mockMvc.perform(get("/api/case")
                        .param("startDate", LocalDateTime.now().minusDays(7).toString())
                        .param("endDate", LocalDateTime.now().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.id == '" + heartCase.getId() + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.id == '" + brainCase.getId() + "')]").exists());

        // Test date range with other filters
        mockMvc.perform(get("/api/case")
                        .param("startDate", LocalDateTime.now().minusDays(7).toString())
                        .param("endDate", LocalDateTime.now().toString())
                        .param("gender", "Male"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(heartCase.getId()));

        // Test date range with no matches
        mockMvc.perform(get("/api/case")
                        .param("startDate", LocalDateTime.now().minusDays(30).toString())
                        .param("endDate", LocalDateTime.now().minusDays(20).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldPaginateCasesCorrectly() throws Exception {
        // 1. Create user
        UserInfoDTO user = testUtil.createUser();

        // 2. Create multiple patients
        Patient[] patients = new Patient[5];
        for (int i = 0; i < 5; i++) {
            patients[i] = testUtil.createPatient("MRN" + i, "Male", 30);
        }

        // 3. Create multiple cases (10 cases total)
        String[] caseIds = new String[10];
        for (int i = 0; i < 10; i++) {
            Case newCase = testUtil.createCaseWithPatientAndDate(
                CaseType.PATHOLOGY,
                patients[i % 5].getId(),
                LocalDateTime.now().minusDays(i)
            );
            caseIds[i] = newCase.getId();
            testUtil.createInitialHistory(newCase.getId(), user.getId());
        }

        // 4. Test first page with size 3
        mockMvc.perform(get("/api/case")
                .param("page", "0")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(4))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.numberOfElements").value(3));

        // 5. Test middle page
        mockMvc.perform(get("/api/case")
                .param("page", "1")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.number").value(1));

        // 6. Test last page
        mockMvc.perform(get("/api/case")
                .param("page", "3")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.first").value(false))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.number").value(3));

        // 7. Test with different page size
        mockMvc.perform(get("/api/case")
                .param("page", "0")
                .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10))
                .andExpect(jsonPath("$.totalPages").value(2));


        // 8. Test with filters
        mockMvc.perform(get("/api/case")
                .param("page", "0")
                .param("size", "5")
                .param("gender", "Male"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.totalElements").value(10));
    }

    @Test
    @Order(9)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFilterAgesNumerically() throws Exception {
        // 1. Create user
        UserInfoDTO user = testUtil.createUser();

        // 2. Create patients with different ages including edge cases
        Patient patient1 = testUtil.createPatient("MRN123", "Male", 35);    // Within range
        Patient patient2 = testUtil.createPatient("MRN456", "Male", 342);   // Should not be in range
        Patient patient3 = testUtil.createPatient("MRN789", "Male", 3);     // Should not be in range
        Patient patient4 = testUtil.createPatient("MRN012", "Male", 40);    // Edge of range
        Patient patient5 = testUtil.createPatient("MRN345", "Male", 30);    // Edge of range

        // 3. Create cases for each patient
        Case case1 = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient1.getId());
        Case case2 = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient2.getId());
        Case case3 = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient3.getId());
        Case case4 = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient4.getId());
        Case case5 = testUtil.createCaseWithPatient(CaseType.PATHOLOGY, patient5.getId());

        testUtil.createInitialHistory(case1.getId(), user.getId());
        testUtil.createInitialHistory(case2.getId(), user.getId());
        testUtil.createInitialHistory(case3.getId(), user.getId());
        testUtil.createInitialHistory(case4.getId(), user.getId());
        testUtil.createInitialHistory(case5.getId(), user.getId());

        // 4. Test age range filter (30-40)
        mockMvc.perform(get("/api/case")
                .param("minAge", "30")
                .param("maxAge", "40"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[*].patientDetailsDTO.age", containsInAnyOrder(30, 35, 40)))
                .andExpect(jsonPath("$.content[*].patientDetailsDTO.age", not(containsInAnyOrder(3, 342))));

        // 5. Test single digit age
        mockMvc.perform(get("/api/case")
                .param("minAge", "1")
                .param("maxAge", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].patientDetailsDTO.age").value("3"));

        // 6. Test exact age match
        mockMvc.perform(get("/api/case")
                .param("minAge", "342")
                .param("maxAge", "342"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].patientDetailsDTO.age").value("342"));
    }

    @Test
    @Order(10)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldIncludeTransferDetailsInCaseDTO() throws Exception {
        // 1. Create users
        UserInfoDTO user1 = testUtil.createUser("pathologist1@example.com", "John", "Doe", UserRole.PATHOLOGIST);
        UserInfoDTO user2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);
        User loggedInUser1 = userService.getUserByUsername(user1.getEmail());
        User loggedInUser2 = userService.getUserByUsername(user2.getEmail());

        // 2. Create a case
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        testUtil.createInitialHistory(caseId, user1.getId());

        // 3. Assign case to user1
        historyService.assignCase(caseId, loggedInUser1);

        // 4. Create slides and annotations for the case
        PatientSlide slide = testUtil.createPatientSlide(caseId);
        testUtil.createAnnotationsForSlide(slide.getId(), user1.getEmail());
        testUtil.completeAllSlides(caseId);

        // 5. Set diagnosis and feedback
        testUtil.createDiagnosis(caseId, user1.getId());
        testUtil.createFeedback(caseId, user1.getId());

        // 6. Transfer case from user1 to user2
        historyService.transferCase(caseId, loggedInUser1, user2.getId());

        // 7. Verify transfer details in REFERRED state
        mockMvc.perform(get("/api/case/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferDetails").exists())
                .andExpect(jsonPath("$.transferDetails.transferredBy.fullName").value(user1.getFullName()))
                .andExpect(jsonPath("$.transferDetails.transferredBy.userRole").value(user1.getRole()))
                .andExpect(jsonPath("$.transferDetails.transferredTo.fullName").value(user2.getFullName()))
                .andExpect(jsonPath("$.transferDetails.transferredTo.userRole").value(user2.getRole()));

        // 8. Complete the case
        testUtil.createDiagnosis(caseId, user2.getId());
        testUtil.createFeedback(caseId, user2.getId());
        testUtil.completeAllSlides(caseId);
        historyService.completeCase(caseId, loggedInUser2);

        // 9. Verify transfer details persist in COMPLETE state
        mockMvc.perform(get("/api/case/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferDetails").exists())
                .andExpect(jsonPath("$.transferDetails.transferredBy.fullName").value(user1.getFullName()))
                .andExpect(jsonPath("$.transferDetails.transferredBy.userRole").value(user1.getRole()))
                .andExpect(jsonPath("$.transferDetails.transferredTo.fullName").value(user2.getFullName()))
                .andExpect(jsonPath("$.transferDetails.transferredTo.userRole").value(user2.getRole()));

        // 10. Create a new case without transfer to verify no transfer details
        String newCaseId = testUtil.createCase(CaseType.PATHOLOGY);
        testUtil.createInitialHistory(newCaseId, user1.getId());

        // 11. Verify no transfer details for new case
        mockMvc.perform(get("/api/case/" + newCaseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferDetails").doesNotExist());

        // 12. Verify transfer details in filtered search
        mockMvc.perform(get("/api/case")
                        .with(user(loggedInUser1))
                .param("status", CaseStatus.COMPLETE.toString())
                .param("type", CaseType.PATHOLOGY.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].transferDetails").exists())
                .andExpect(jsonPath("$.content[0].transferDetails.transferredBy.fullName").value(user1.getFullName()))
                .andExpect(jsonPath("$.content[0].transferDetails.transferredTo.fullName").value(user2.getFullName()));
    }

}

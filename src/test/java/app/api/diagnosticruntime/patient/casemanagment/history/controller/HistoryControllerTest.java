package app.api.diagnosticruntime.patient.casemanagment.history.controller;

import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.patient.casemanagment.dto.CaseInfoDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.patient.casemanagment.history.repository.HistoryRepository;
import app.api.diagnosticruntime.patient.casemanagment.history.service.HistoryService;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.service.UserService;
import app.api.diagnosticruntime.util.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none") // Avoid replacing database
public class HistoryControllerTest {


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HistoryRepository historyRepository;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private UserService userService;

    @Autowired
    private TestUtil testUtil;


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
    void shouldAssignCase() throws Exception {

        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 2. Insert initial NEW history entry
        testUtil.createInitialHistory(caseId, user.getId());

        // 3. Assign case
        mockMvc.perform(post("/api/case/" + caseId + "/assign"))
                .andExpect(status().isOk());

        // 4. Verify case is now IN_PROGRESS
        String note = "User with id:"+user.getId()+" and email:"+user.getEmail()+" assigned the case to themselves";
        List<History> histories = historyRepository.findAll();

        Optional<Case> updatedCase = testUtil.getCaseById(caseId);
        assertEquals(2, histories.size());
        assertEquals(CaseStatus.IN_PROGRESS, histories.get(1).getNewStatus());
        assertEquals(CaseStatus.NEW, histories.get(1).getPreviousStatus());
        assertEquals(user.getId(), histories.get(1).getActionByPathologistId());
        assertEquals(note, histories.get(1).getNote());
        assertTrue(updatedCase.isPresent());
        assertEquals(CaseStatus.IN_PROGRESS, updatedCase.get().getStatus());
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldThrowException_WhenAssigningCaseWithNoHistory() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // (NO INITIAL HISTORY CREATED)

        // 2. Attempt to assign case → should fail
        mockMvc.perform(post("/api/case/" + caseId + "/assign"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Assigning a case with no previous record",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldAssignCase_WhenPreviousStatusIsComplete() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 2. Insert initial COMPLETE history entry
        testUtil.createHistory(caseId, CaseStatus.COMPLETE, user.getId());

        // 3. Assign case
        mockMvc.perform(post("/api/case/" + caseId + "/assign"))
                .andExpect(status().isOk());

        // 4. Verify case is now IN_PROGRESS
        String note = "User with id:" + user.getId() + " and email:" + user.getEmail() +
                " assigned the case to themselves after removing it from state: COMPLETE";

        List<History> histories = historyRepository.findAll();
        Optional<Case> updatedCase = testUtil.getCaseById(caseId);
        assertEquals(2, histories.size());
        assertEquals(CaseStatus.IN_PROGRESS, histories.get(1).getNewStatus());
        assertEquals(CaseStatus.NEW, histories.get(1).getPreviousStatus());
        assertEquals(user.getId(), histories.get(1).getActionByPathologistId());
        assertEquals(note, histories.get(1).getNote());
        assertTrue(updatedCase.isPresent());
        assertEquals(CaseStatus.IN_PROGRESS, updatedCase.get().getStatus());
    }

    @Test
    @Order(4)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldThrowException_WhenAssigningCaseWithInvalidPreviousStatus() throws Exception {
        // Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // Insert history with an invalid status (e.g., REFERRED)
        testUtil.createHistory(caseId, CaseStatus.REFERRED, user.getId());

        // Attempt to assign case → should fail
        mockMvc.perform(post("/api/case/" + caseId + "/assign"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Cannot assign case if Previous case type isn't NEW or COMPLETE",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(5)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldUnassignCase() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 2. Insert initial NEW history entry
        testUtil.createInitialHistory(caseId, user.getId());

        // 3. Assign case first
        mockMvc.perform(post("/api/case/" + caseId + "/assign"))
                .andExpect(status().isOk());

        // 4. Unassign case
        mockMvc.perform(post("/api/case/" + caseId + "/unassign"))
                .andExpect(status().isOk());

        // 5. Verify case history
        List<History> histories = historyRepository.findAll();
        assertEquals(3, histories.size()); // NEW → IN_PROGRESS → NEW
        assertEquals(CaseStatus.IN_PROGRESS, histories.get(1).getNewStatus());
        assertEquals(CaseStatus.NEW, histories.get(2).getNewStatus());
    }


    @Test
    @Order(6)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldThrowException_WhenUnassigningCaseWithNoHistory() throws Exception {
        // Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // NO history created

        // Attempt to unassign → should fail
        mockMvc.perform(post("/api/case/" + caseId + "/unassign"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Case must have a previous state for it to be un-assigned",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(7)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldThrowException_WhenUnassigningInvalidStatus() throws Exception {
        // Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // Insert invalid history state (e.g., REFERRED)
        testUtil.createHistory(caseId, CaseStatus.REFERRED, user.getId());

        // Attempt to unassign → should fail
        mockMvc.perform(post("/api/case/" + caseId + "/unassign"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Cannot un-assign or create case, this state change from REFERRED to IN_PROGRESS is not allowed",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(8)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldUnassignCase_AndDeleteAnnotationsAndComments_AndUpdatePatientSlidesStatusToNEW() throws Exception {
        // Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // Create PatientSlide
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        // Insert history: NEW → IN_PROGRESS
        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());

        // Add annotations and comments
        testUtil.createAnnotationsForSlide(patientSlide.getId(), user.getEmail());
        testUtil.createCommentsForCase(caseId, user.getEmail());

        // Verify annotations/comments exist
        assertEquals(1, testUtil.getAllAnnotationsForPatientSlide(patientSlide.getId()).size());
        assertEquals(1, testUtil.getAllCommentsByCaseId(caseId).size());

        // Unassign case with deleteAnnotationsAndComments = true
        mockMvc.perform(post("/api/case/" + caseId + "/unassign")
                        .param("deleteAnnotationsAndComments", "true"))
                .andExpect(status().isOk());

        // Verify annotations and comments are deleted
        assertEquals(0, testUtil.getAllAnnotationsForPatientSlide(patientSlide.getId()).size());
        assertEquals(0, testUtil.getAllCommentsByCaseId(caseId).size());

        List<PatientSlide> patientSlides = testUtil.getAllPatientSlidesByCaseId(caseId);
        patientSlides.forEach(p -> assertEquals(p.getStatus(), PatientSlideStatus.NEW));

        // Verify case history updated
        List<History> histories = historyRepository.findAll();
        assertEquals(3, histories.size());
        assertEquals(CaseStatus.NEW, histories.get(2).getNewStatus());
    }

    @Test
    @Order(9)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldUnassignCase_WithoutDeletingAnnotationsAndComments() throws Exception {
        // Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // Create PatientSlide
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        // Insert history: NEW → IN_PROGRESS
        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());

        // Add annotations and comments
        testUtil.createAnnotationsForSlide(patientSlide.getId(), user.getEmail());
        testUtil.createCommentsForCase(caseId, user.getEmail());

        // Verify annotations/comments exist
        assertEquals(1, testUtil.getAllAnnotationsForPatientSlide(patientSlide.getId()).size());
        assertEquals(1, testUtil.getAllCommentsByCaseId(caseId).size());

        // Unassign case with deleteAnnotationsAndComments = false
        mockMvc.perform(post("/api/case/" + caseId + "/unassign")
                        .param("deleteAnnotationsAndComments", "false"))
                .andExpect(status().isOk());

        // Verify annotations and comments are NOT deleted
        assertEquals(1, testUtil.getAllAnnotationsForPatientSlide(patientSlide.getId()).size());
        assertEquals(1, testUtil.getAllCommentsByCaseId(caseId).size());

        // Verify case history updated
        List<History> histories = historyRepository.findAll();
        assertEquals(3, histories.size());
        assertEquals(CaseStatus.NEW, histories.get(2).getNewStatus());
    }

    @Test
    @Order(10)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldTransferCase_Success() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        // 2. Insert history: NEW → IN_PROGRESS
        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());

        // 3. Add annotation, diagnosis, and feedback
        testUtil.createAnnotationsForSlide(patientSlide.getId(), user.getEmail());
        testUtil.completeAllSlides(caseId);
        testUtil.createDiagnosis(caseId, user.getId());
        testUtil.createFeedback(caseId, user.getId());

        // 4. Transfer case
        mockMvc.perform(post("/api/case/" + caseId + "/transfer")
                        .param("targetPathologistId", "pathologist2"))
                .andExpect(status().isOk());

        // 5. Verify case transferred
        List<History> histories = historyRepository.findAll();
        assertEquals(3, histories.size());
        assertEquals(CaseStatus.REFERRED, histories.get(2).getNewStatus());

        List<PatientSlide> patientSlides = testUtil.getAllPatientSlidesByCaseId(caseId);
        patientSlides.forEach(p -> assertEquals(PatientSlideStatus.IN_PROGRESS, p.getStatus()));
    }

    @Test
    @Order(11)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldNotFailTransferCase_NoSlides() throws Exception {
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());

        testUtil.completeAllSlides(caseId);
        testUtil.createDiagnosis(caseId, user.getId());
        testUtil.createFeedback(caseId, user.getId());

        mockMvc.perform(post("/api/case/" + caseId + "/transfer")
                        .param("targetPathologistId", "pathologist2"))
                .andExpect(status().isOk());
    }

    @Test
    @Order(12)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailTransferCase_IncompleteSlides() throws Exception {
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());

        testUtil.createAnnotationsForSlide(patientSlide.getId(), user.getEmail());
        testUtil.createDiagnosis(caseId, user.getId());
        testUtil.createFeedback(caseId, user.getId());

        mockMvc.perform(post("/api/case/" + caseId + "/transfer")
                        .param("targetPathologistId", "pathologist2"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Case cannot be referred until all slides are completed",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(13)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldCompleteCase_Success() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);
        testUtil.completeAllSlides(caseId);

        // 2. Insert history: NEW → IN_PROGRESS → REFERRED
        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());
        testUtil.createHistory(caseId, CaseStatus.REFERRED, user.getId());

        // 3. Add diagnosis and feedback
        testUtil.createDiagnosis(caseId, user.getId());
        testUtil.createFeedback(caseId, user.getId());

        // 4. Complete case
        mockMvc.perform(post("/api/case/" + caseId + "/complete"))
                .andExpect(status().isOk());

        // 5. Verify case completed
        List<History> histories = historyRepository.findAll();
        assertEquals(4, histories.size());
        assertEquals(CaseStatus.COMPLETE, histories.get(3).getNewStatus());
    }

    @Test
    @Order(14)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailCompleteCase_MissingDiagnosis() throws Exception {
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());
        testUtil.createHistory(caseId, CaseStatus.REFERRED, user.getId());

        testUtil.createFeedback(caseId, user.getId());

        mockMvc.perform(post("/api/case/" + caseId + "/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("User must set a diagnosis for the case before completing it",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(15)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailCompleteCase_MissingFeedback() throws Exception {
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());
        testUtil.createHistory(caseId, CaseStatus.REFERRED, user.getId());

        testUtil.createDiagnosis(caseId, user.getId());

        mockMvc.perform(post("/api/case/" + caseId + "/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("User must set the case difficulty before completing it",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(16)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailCompleteCase_InvalidState() throws Exception {
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());

        mockMvc.perform(post("/api/case/" + caseId + "/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Case must have a valid REFERRED state for it to be transferable",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(17)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldFailCompleteCase_SlidesNotCompleted() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        // 2. Insert history: NEW → IN_PROGRESS → REFERRED
        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());
        testUtil.createHistory(caseId, CaseStatus.REFERRED, user.getId());

        // 3. Add diagnosis and feedback
        testUtil.createDiagnosis(caseId, user.getId());
        testUtil.createFeedback(caseId, user.getId());

        // 4. Complete case
        mockMvc.perform(post("/api/case/" + caseId + "/complete"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("Case cannot be completed until all slides are marked as completed",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(18)
    void shouldGetCorrectCaseCounts_ForAllStates() throws Exception {
        // 1. Create user and cases
        UserInfoDTO user1 = testUtil.createUser();
        UserInfoDTO user2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);

        String caseId1 = testUtil.createCase(CaseType.PATHOLOGY);
        String caseId2 = testUtil.createCase(CaseType.PATHOLOGY);
        String caseId3 = testUtil.createCase(CaseType.RADIOLOGY);

        // 2. Create history entries
        testUtil.createInitialHistory(caseId1, user1.getId());
        testUtil.createInitialHistory(caseId2, user1.getId());
        testUtil.createInitialHistory(caseId3, user1.getId());

        // 3. Get case counts for PATHOLOGY type
        CaseInfoDTO pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user1.getEmail());

        // 4. Verify PATHOLOGY counts
        assertEquals(2, pathologyCounts.getTotal());
        assertEquals(2, pathologyCounts.getNewCases());
        assertEquals(0, pathologyCounts.getInProgress());
        assertEquals(0, pathologyCounts.getCompleted());

        // 5. Assign one case
        User loggedInUser =  userService.getUserByUsername(user1.getEmail());
        User loggedInUser2 =  userService.getUserByUsername(user2.getEmail());
        historyService.assignCase(caseId1, loggedInUser);

        // 6. Get updated counts
        pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user1.getEmail());

        // 7. Verify updated counts
        assertEquals(2, pathologyCounts.getTotal());
        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(1, pathologyCounts.getInProgress());
        assertEquals(0, pathologyCounts.getCompleted());


        // 8. Set diagnosis and feedback and annotations
        PatientSlide slide = testUtil.createPatientSlide(caseId1);
        testUtil.createAnnotationsForSlide(slide.getId(), user1.getEmail());
        testUtil.createDiagnosis(caseId1, user1.getId());
        testUtil.createFeedback(caseId1, user1.getId());
        testUtil.completeAllSlides(caseId1);
        historyService.transferCase(caseId1, loggedInUser, user2.getId()); // Transfer to another user for testing

        // 9. Get counts after transfer
        pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user1.getEmail());

        // 10. Verify counts after transfer
        assertEquals(2, pathologyCounts.getTotal());
        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(0, pathologyCounts.getInProgress());
        assertEquals(1, pathologyCounts.getReferred());
        assertEquals(0, pathologyCounts.getIncoming());
        assertEquals(0, pathologyCounts.getCompleted());

        // Check counts for user2
        pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user2.getEmail());

        // 11. Verify counts after transfer
        assertEquals(2, pathologyCounts.getTotal());
        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(0, pathologyCounts.getInProgress());
        assertEquals(0, pathologyCounts.getReferred());
        assertEquals(1, pathologyCounts.getIncoming());
        assertEquals(0, pathologyCounts.getCompleted());

        // 12. Complete the case

        testUtil.createAnnotationsForSlide(slide.getId(), user2.getEmail());
        testUtil.createDiagnosis(caseId1, user2.getId());
        testUtil.createFeedback(caseId1, user2.getId());
        testUtil.completeAllSlides(caseId1);
        historyService.completeCase(caseId1, loggedInUser2);

        // 13. Get final counts
        pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user1.getEmail());

        // 14. Verify final counts
        assertEquals(2, pathologyCounts.getTotal());
        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(0, pathologyCounts.getInProgress());
        assertEquals(0, pathologyCounts.getReferred());
        assertEquals(0, pathologyCounts.getIncoming());
        assertEquals(1, pathologyCounts.getCompleted());

        // 13. Get final counts of second user
        pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user2.getEmail());

        // 14. Verify final counts
        assertEquals(2, pathologyCounts.getTotal());
        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(0, pathologyCounts.getInProgress());
        assertEquals(0, pathologyCounts.getReferred());
        assertEquals(0, pathologyCounts.getIncoming());
        assertEquals(1, pathologyCounts.getCompleted());

        // 15. Verify RADIOLOGY counts remain unchanged
        CaseInfoDTO radiologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.RADIOLOGY, user1.getEmail());
        assertEquals(1, radiologyCounts.getTotal());
        assertEquals(1, radiologyCounts.getNewCases());
        assertEquals(0, radiologyCounts.getInProgress());
        assertEquals(0, radiologyCounts.getCompleted());

    }

    @Test
    @Order(19)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetLatestTransferHistory() throws Exception {
        // 1. Create users
        UserInfoDTO user1 = testUtil.createUser("pathologist1@example.com", "John", "Doe", UserRole.PATHOLOGIST);
        UserInfoDTO user2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);

        // 2. Create case
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 3. Create initial history
        testUtil.createInitialHistory(caseId, user1.getId());

        // 4. Assign case to user1
        User loggedInUser1 = userService.getUserByUsername(user1.getEmail());
        historyService.assignCase(caseId, loggedInUser1);

        // 5. Create necessary items for transfer
        PatientSlide slide = testUtil.createPatientSlide(caseId);
        testUtil.createAnnotationsForSlide(slide.getId(), user1.getEmail());
        testUtil.completeAllSlides(caseId);
        testUtil.createDiagnosis(caseId, user1.getId());
        testUtil.createFeedback(caseId, user1.getId());

        // 6. Transfer case from user1 to user2
        historyService.transferCase(caseId, loggedInUser1, user2.getId());

        // 7. Get and verify latest transfer history
        mockMvc.perform(get("/api/case/" + caseId + "/latest-transfer-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transferredBy.fullName").value("John Doe"))
                .andExpect(jsonPath("$.transferredBy.userRole").value("PATHOLOGIST"))
                .andExpect(jsonPath("$.transferredTo.fullName").value("Jane Smith"))
                .andExpect(jsonPath("$.transferredTo.userRole").value("PATHOLOGIST"));
    }

    @Test
    @Order(20)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldThrowException_WhenNoTransferHistory() throws Exception {
        // 1. Create user and case
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);

        // 2. Create initial history (but no transfers)
        testUtil.createInitialHistory(caseId, user.getId());

        // 3. Verify exception is thrown
        mockMvc.perform(get("/api/case/" + caseId + "/latest-transfer-status"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("This case has not been transferred to anyone",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(21)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldThrowException_WhenTransferUserDoesNotExist() throws Exception {
        // 1. Create users
        UserInfoDTO user1 = testUtil.createUser("pathologist1@example.com", "John", "Doe", UserRole.PATHOLOGIST);
        String nonExistentUserId = "non_existent_user_id";

        // 2. Create case and history with non-existent user
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        History history = new History();
        history.setCaseId(caseId);
        history.setNewStatus(CaseStatus.REFERRED);
        history.setActionByPathologistId(user1.getId());
        history.setTransferredToPathologistId(nonExistentUserId);
        historyRepository.save(history);

        // 3. Verify exception is thrown
        mockMvc.perform(get("/api/case/" + caseId + "/latest-transfer-status"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof IllegalArgumentException))
                .andExpect(result -> assertEquals("This user of id: " + nonExistentUserId + " doesn't exist",
                        result.getResolvedException().getMessage()));
    }

    @Test
    @Order(22)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetCaseInfoByType() throws Exception {
        // 1. Create users
        UserInfoDTO user = testUtil.createUser("pathologist@example.com", "John", "Doe", UserRole.PATHOLOGIST);
        UserInfoDTO user2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);

        // 2. Create multiple cases with different statuses
        String case1Id = testUtil.createCase(CaseType.PATHOLOGY);
        String case2Id = testUtil.createCase(CaseType.PATHOLOGY);
        String case3Id = testUtil.createCase(CaseType.PATHOLOGY);
        String case4Id = testUtil.createCase(CaseType.RADIOLOGY); // Different type
        String case5Id = testUtil.createCase(CaseType.PATHOLOGY);

        // 3. Create initial history entries
        testUtil.createInitialHistory(case1Id, user.getId());
        testUtil.createInitialHistory(case2Id, user.getId());
        testUtil.createInitialHistory(case3Id, user.getId());
        testUtil.createInitialHistory(case4Id, user.getId());
        testUtil.createInitialHistory(case5Id, user.getId());

        // 4. Set up different case statuses
        User loggedInUser = userService.getUserByUsername(user.getEmail());
        User loggedInUser2 = userService.getUserByUsername(user2.getEmail());

        // Assign case1 (IN_PROGRESS)
        historyService.assignCase(case1Id, loggedInUser);

        // Complete case2 (COMPLETE)
        // First assign to user1
        historyService.assignCase(case2Id, loggedInUser);
        PatientSlide slide2 = testUtil.createPatientSlide(case2Id);
        testUtil.createAnnotationsForSlide(slide2.getId(), user.getEmail());
        testUtil.completeAllSlides(case2Id);
        testUtil.createDiagnosis(case2Id, user.getId());
        testUtil.createFeedback(case2Id, user.getId());
        // Transfer to user2
        historyService.transferCase(case2Id, loggedInUser, user2.getId());
        // User2 assigns and completes
        testUtil.createDiagnosis(case2Id, user2.getId());
        testUtil.createFeedback(case2Id, user2.getId());
        testUtil.completeAllSlides(case2Id);
        historyService.completeCase(case2Id, loggedInUser2);

        // Transfer case3 (REFERRED)
        historyService.assignCase(case3Id, loggedInUser);
        PatientSlide slide3 = testUtil.createPatientSlide(case3Id);
        testUtil.createAnnotationsForSlide(slide3.getId(), user.getEmail());
        testUtil.completeAllSlides(case3Id);
        testUtil.createDiagnosis(case3Id, user.getId());
        testUtil.createFeedback(case3Id, user.getId());
        historyService.transferCase(case3Id, loggedInUser, user2.getId());

        // 5. Verify case info for PATHOLOGY type
        mockMvc.perform(get("/api/case/type/PATHOLOGY/status/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCases").value(1))
                .andExpect(jsonPath("$.inProgress").value(1))
                .andExpect(jsonPath("$.referred").value(1))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.incoming").value(0))
                .andExpect(jsonPath("$.total").value(4));

        CaseInfoDTO pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user.getEmail());

        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(1, pathologyCounts.getInProgress());
        assertEquals(1, pathologyCounts.getReferred());
        assertEquals(1, pathologyCounts.getCompleted());
        assertEquals(0, pathologyCounts.getIncoming());
        assertEquals(4, pathologyCounts.getTotal());


        mockMvc.perform(get("/api/case/type/PATHOLOGY/status/count")
                .with(user(user2.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCases").value(1))
                .andExpect(jsonPath("$.inProgress").value(0))
                .andExpect(jsonPath("$.referred").value(0))
                .andExpect(jsonPath("$.completed").value(1))
                .andExpect(jsonPath("$.incoming").value(1))
                .andExpect(jsonPath("$.total").value(3));

        pathologyCounts = historyService.getCaseInfoByTypeAndUser(CaseType.PATHOLOGY, user2.getEmail());

        assertEquals(1, pathologyCounts.getNewCases());
        assertEquals(0, pathologyCounts.getInProgress());
        assertEquals(0, pathologyCounts.getReferred());
        assertEquals(1, pathologyCounts.getCompleted());
        assertEquals(1, pathologyCounts.getIncoming());
        assertEquals(3, pathologyCounts.getTotal());

        // 6. Verify case info for RADIOLOGY type
        mockMvc.perform(get("/api/case/type/RADIOLOGY/status/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCases").value(1))
                .andExpect(jsonPath("$.inProgress").value(0))
                .andExpect(jsonPath("$.referred").value(0))
                .andExpect(jsonPath("$.incoming").value(0))
                .andExpect(jsonPath("$.completed").value(0))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @Order(23)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldReturnZeroCounts_WhenNoCasesExist() throws Exception {
        // Create user but no cases
        testUtil.createUser("pathologist@example.com", "John", "Doe", UserRole.PATHOLOGIST);

        mockMvc.perform(get("/api/case/type/PATHOLOGY/status/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newCases").value(0))
                .andExpect(jsonPath("$.inProgress").value(0))
                .andExpect(jsonPath("$.referred").value(0))
                .andExpect(jsonPath("$.completed").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @Order(24)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldNotFailTransferCase_OnSlidesWithNoAnnotations() throws Exception {
        UserInfoDTO user = testUtil.createUser();
        String caseId = testUtil.createCase(CaseType.PATHOLOGY);
        PatientSlide patientSlide = testUtil.createPatientSlide(caseId);

        testUtil.createHistory(caseId, CaseStatus.NEW, user.getId());
        testUtil.createHistory(caseId, CaseStatus.IN_PROGRESS, user.getId());

//        testUtil.createAnnotationsForSlide(patientSlide.getId(), user.getEmail());
        testUtil.createDiagnosis(caseId, user.getId());
        testUtil.createFeedback(caseId, user.getId());
        testUtil.completeAllSlides(caseId);

        mockMvc.perform(post("/api/case/" + caseId + "/transfer")
                        .param("targetPathologistId", "pathologist2"))
                .andExpect(status().isOk());
    }
}

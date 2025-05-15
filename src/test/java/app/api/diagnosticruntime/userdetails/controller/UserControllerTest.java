package app.api.diagnosticruntime.userdetails.controller;

import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.userdetails.dto.UserFilter;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.model.UserStatus;
import app.api.diagnosticruntime.userdetails.service.UserService;
import app.api.diagnosticruntime.util.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void shouldGetAllUsersWithFilters() throws Exception {
        // 1. Create multiple users with different roles and statuses
        UserInfoDTO adminUser = testUtil.createUser("admin@example.com", "Admin", "User", UserRole.ADMIN);
        UserInfoDTO pathologist1 = testUtil.createUser("pathologist1@example.com", "John", "Doe", UserRole.PATHOLOGIST);
        UserInfoDTO pathologist2 = testUtil.createUser("pathologist2@example.com", "Jane", "Smith", UserRole.PATHOLOGIST);
        UserInfoDTO radiologist = testUtil.createUser("radiologist@example.com", "Alex", "Brown", UserRole.RADIOLOGIST);

        // 2. Update some users' statuses
        testUtil.updateUserStatus(pathologist1.getId(), UserStatus.INACTIVE);
        testUtil.updateUserStatus(pathologist2.getId(), UserStatus.REJECTED);

        // 3. Test filtering by role (PATHOLOGIST)
        mockMvc.perform(get("/api/user")
                        .param("role", UserRole.PATHOLOGIST.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.email == 'pathologist1@example.com')]").exists())
                .andExpect(jsonPath("$.content[?(@.email == 'pathologist2@example.com')]").exists())
                .andExpect(jsonPath("$.content[?(@.email == 'admin@example.com')]").doesNotExist());

        // 4. Test filtering by status (ACTIVE)
        mockMvc.perform(get("/api/user")
                        .param("status", UserStatus.ACTIVE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[?(@.email == 'admin@example.com')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.email == 'radiologist@example.com')]").exists());

        // 5. Test filtering by fullname (case-insensitive)
        mockMvc.perform(get("/api/user")
                        .param("fullname", "john"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("pathologist1@example.com"));

        // 6. Test combination of filters
        mockMvc.perform(get("/api/user")
                        .param("role", UserRole.PATHOLOGIST.toString())
                        .param("status", UserStatus.INACTIVE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("pathologist1@example.com"));

        // 7. Test pagination
        mockMvc.perform(get("/api/user")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3)) // Total users excluding logged-in user
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"PATHOLOGIST"})
    void shouldNotSeeLoggedInUserInResults() throws Exception {
        // 1. Create multiple users
        UserInfoDTO loggedInUser = testUtil.createUser("pathologist@example.com", "Logged", "In", UserRole.PATHOLOGIST);
        UserInfoDTO otherUser1 = testUtil.createUser("other1@example.com", "Other", "One", UserRole.PATHOLOGIST);
        UserInfoDTO otherUser2 = testUtil.createUser("other2@example.com", "Other", "Two", UserRole.PATHOLOGIST);

        // 2. Get all users and verify logged-in user is not in results
        mockMvc.perform(get("/api/user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[?(@.email == 'pathologist@example.com')]").doesNotExist())
                .andExpect(jsonPath("$.content[?(@.email == 'other1@example.com')]").exists())
                .andExpect(jsonPath("$.content[?(@.email == 'other2@example.com')]").exists());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "admin@example.com", roles = {"ADMIN"})
    void shouldHandleEmptyResults() throws Exception {
        // 1. Create only the logged-in user
        UserInfoDTO adminUser = testUtil.createUser("admin@example.com", "Admin", "User", UserRole.ADMIN);

        // 2. Test with non-matching filters
        mockMvc.perform(get("/api/user")
                        .param("role", UserRole.RADIOLOGIST.toString())
                        .param("status", UserStatus.ACTIVE.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
} 
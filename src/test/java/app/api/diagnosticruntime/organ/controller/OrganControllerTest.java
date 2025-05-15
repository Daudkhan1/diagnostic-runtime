package app.api.diagnosticruntime.organ.controller;

import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.organ.service.OrganService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none")
public class OrganControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganService organService;

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
    void shouldReturnEmptyListWhenNoOrgansExist() throws Exception {
        mockMvc.perform(get("/api/organ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("No organs found"));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldReturnAllOrganNames() throws Exception {
        // Create some test organs
        organService.getOrCreateOrganName("Brain");
        organService.getOrCreateOrganName("Liver");
        organService.getOrCreateOrganName("Heart");

        mockMvc.perform(get("/api/organ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0]").value("BRAIN"))  // Alphabetically sorted
                .andExpect(jsonPath("$.data[1]").value("HEART"))
                .andExpect(jsonPath("$.data[2]").value("LIVER"));
    }

    @Test
    @Order(3)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldReturnOrganById() throws Exception {
        // Create an organ and get its ID
        String organId = organService.getOrCreateOrganId("BRAIN");

        mockMvc.perform(get("/api/organ/" + organId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(organId))
                .andExpect(jsonPath("$.data.name").value("BRAIN"));
    }

    @Test
    @Order(4)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldReturn404WhenOrganNotFound() throws Exception {
        String nonExistentId = "non_existent_id";

        mockMvc.perform(get("/api/organ/" + nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Organ not found with id: " + nonExistentId));
    }


    @Test
    @Order(5)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldReturnSortedOrganNames() throws Exception {
        // Create organs in non-alphabetical order
        organService.getOrCreateOrganName("ZINC");
        organService.getOrCreateOrganName("ALPHA");
        organService.getOrCreateOrganName("BETA");

        mockMvc.perform(get("/api/organ")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0]").value("ALPHA"))
                .andExpect(jsonPath("$.data[1]").value("BETA"))
                .andExpect(jsonPath("$.data[2]").value("ZINC"));
    }
} 
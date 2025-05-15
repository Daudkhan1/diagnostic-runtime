package app.api.diagnosticruntime.disease.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.repository.SubtypeRepository;
import app.api.diagnosticruntime.organ.service.OrganService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none")
public class SubtypeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganService organService;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private SubtypeRepository subtypeRepository;

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
    void shouldGetAllSubtypesByOrgan() throws Exception {
        // Create organ
        String organName = "LIVER";
        organService.getOrCreateOrganName(organName);

        // Create test subtypes
        SubtypeDTO subtype1 = createTestSubtype("Subtype1", organName);
        SubtypeDTO subtype2 = createTestSubtype("Subtype2", organName);

        // Get all subtypes for the organ
        MvcResult result = mockMvc.perform(get("/api/subtype/organ/" + organName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        List<SubtypeDTO> responseSubtypes = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<List<SubtypeDTO>>>() {}
        ).getData();

        // Verify response
        assertEquals(2, responseSubtypes.size());
        assertTrue(responseSubtypes.stream()
                .anyMatch(s -> s.getName().equals("Subtype1")));
        assertTrue(responseSubtypes.stream()
                .anyMatch(s -> s.getName().equals("Subtype2")));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetSubtypeById() throws Exception {
        // Create organ and subtype
        String organName = "KIDNEY";
        organService.getOrCreateOrganName(organName);
        SubtypeDTO subtype = createTestSubtype("TestSubtype", organName);

        // Get subtype by ID
        MvcResult result = mockMvc.perform(get("/api/subtype/" + subtype.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        SubtypeDTO responseSubtype = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<SubtypeDTO>>() {}
        ).getData();

        // Verify response
        assertEquals("TestSubtype", responseSubtype.getName());
        assertEquals(organName, responseSubtype.getOrganName());
    }

    private SubtypeDTO createTestSubtype(String name, String organName) {
        SubtypeDTO dto = new SubtypeDTO();
        dto.setName(name);
        dto.setOrganName(organName);
        return testUtil.createSubtype(dto);
    }
} 
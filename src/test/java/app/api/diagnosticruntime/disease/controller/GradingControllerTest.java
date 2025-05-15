package app.api.diagnosticruntime.disease.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.repository.GradingRepository;
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
public class GradingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganService organService;

    @Autowired
    private TestUtil testUtil;

    @Autowired
    private GradingRepository gradingRepository;

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
    void shouldGetAllGradingsByOrgan() throws Exception {
        // Create organ and disease spectrum
        String organName = "LIVER";
        organService.getOrCreateOrganName(organName);
        DiseaseSpectrumDTO spectrum = createTestDiseaseSpectrum("TestSpectrum", organName);

        // Create test gradings
        GradingDTO grading1 = createTestGrading("Grade1", spectrum.getName(), organName);
        GradingDTO grading2 = createTestGrading("Grade2", spectrum.getName(), organName);

        // Get all gradings for the organ
        MvcResult result = mockMvc.perform(get("/api/grading/organ/" + organName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        List<GradingDTO> responseGradings = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<List<GradingDTO>>>() {}
        ).getData();

        // Verify response
        assertEquals(2, responseGradings.size());
        assertTrue(responseGradings.stream()
                .anyMatch(g -> g.getName().equals("Grade1")));
        assertTrue(responseGradings.stream()
                .anyMatch(g -> g.getName().equals("Grade2")));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetGradingById() throws Exception {
        // Create organ and disease spectrum
        String organName = "KIDNEY";
        organService.getOrCreateOrganName(organName);
        DiseaseSpectrumDTO spectrum = createTestDiseaseSpectrum("TestSpectrum", organName);

        // Create test grading
        GradingDTO grading = createTestGrading("TestGrade", spectrum.getName(), organName);

        // Get grading by ID
        MvcResult result = mockMvc.perform(get("/api/grading/" + grading.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        GradingDTO responseGrading = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<GradingDTO>>() {}
        ).getData();

        // Verify response
        assertEquals("TestGrade", responseGrading.getName());
        assertEquals("TestSpectrum", responseGrading.getDiseaseSpectrumName());
        assertEquals(organName, responseGrading.getOrganName());
    }

    @Test
    @Order(3)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetAllGradingsByOrganAndDiseaseSpectrum() throws Exception {
        // Create organ and disease spectrums
        String organName = "LUNG";
        organService.getOrCreateOrganName(organName);
        
        // Create two disease spectrums for the same organ
        DiseaseSpectrumDTO spectrum1 = createTestDiseaseSpectrum("Spectrum1", organName);
        DiseaseSpectrumDTO spectrum2 = createTestDiseaseSpectrum("Spectrum2", organName);

        // Create gradings for each spectrum
        GradingDTO grading1 = createTestGrading("Grade1", spectrum1.getName(), organName);
        GradingDTO grading2 = createTestGrading("Grade2", spectrum1.getName(), organName);
        GradingDTO grading3 = createTestGrading("Grade3", spectrum2.getName(), organName);

        // Get gradings for specific organ and spectrum
        MvcResult result = mockMvc.perform(get("/api/grading/organ/" + organName + "/disease-spectrum/" + spectrum1.getName())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        List<GradingDTO> responseGradings = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<List<GradingDTO>>>() {}
        ).getData();

        // Verify response
        assertEquals(2, responseGradings.size());
        assertTrue(responseGradings.stream()
                .anyMatch(g -> g.getName().equals("Grade1")));
        assertTrue(responseGradings.stream()
                .anyMatch(g -> g.getName().equals("Grade2")));
        assertFalse(responseGradings.stream()
                .anyMatch(g -> g.getName().equals("Grade3")));

        // Verify all gradings have correct disease spectrum and organ
        responseGradings.forEach(grading -> {
            assertEquals(spectrum1.getName(), grading.getDiseaseSpectrumName());
            assertEquals(organName, grading.getOrganName());
        });
    }

    private DiseaseSpectrumDTO createTestDiseaseSpectrum(String name, String organName) {
        DiseaseSpectrumDTO dto = new DiseaseSpectrumDTO();
        dto.setName(name);
        dto.setOrganName(organName);
        return testUtil.createDiseaseSpectrum(dto);
    }

    private GradingDTO createTestGrading(String name, String diseaseSpectrumName, String organName) {
        GradingDTO dto = new GradingDTO();
        dto.setName(name);
        dto.setDiseaseSpectrumName(diseaseSpectrumName);
        dto.setOrganName(organName);
        return testUtil.createGrading(dto);
    }
} 
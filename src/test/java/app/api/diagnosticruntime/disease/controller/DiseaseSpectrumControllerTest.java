package app.api.diagnosticruntime.disease.controller;

import app.api.diagnosticruntime.common.dto.ApiResponse;
import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = "spring.test.database.replace=none")
public class DiseaseSpectrumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    void shouldGetAllDiseaseSpectrumsByOrgan() throws Exception {
        // Create organ and disease spectrums
        String organName = "LIVER";
        organService.getOrCreateOrganName(organName);

        // Create test data using annotation creation
        DiseaseSpectrumDTO spectrum1 = createTestDiseaseSpectrum("Spectrum1", organName);
        DiseaseSpectrumDTO spectrum2 = createTestDiseaseSpectrum("Spectrum2", organName);

        // Get all disease spectrums for the organ
        MvcResult result = mockMvc.perform(get("/api/disease-spectrum/organ/" + organName)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        List<DiseaseSpectrumDTO> responseSpectrums = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<List<DiseaseSpectrumDTO>>>() {}
        ).getData();

        // Verify response
        assertEquals(2, responseSpectrums.size());
        assertTrue(responseSpectrums.stream()
                .anyMatch(s -> s.getName().equals("Spectrum1")));
        assertTrue(responseSpectrums.stream()
                .anyMatch(s -> s.getName().equals("Spectrum2")));
    }

    @Test
    @Order(2)
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetDiseaseSpectrumById() throws Exception {
        // Create organ and disease spectrum
        String organName = "KIDNEY";
        organService.getOrCreateOrganName(organName);
        DiseaseSpectrumDTO spectrum = createTestDiseaseSpectrum("TestSpectrum", organName);

        // Get disease spectrum by ID
        MvcResult result = mockMvc.perform(get("/api/disease-spectrum/" + spectrum.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        // Parse response
        String content = result.getResponse().getContentAsString();
        DiseaseSpectrumDTO responseSpectrum = objectMapper.readValue(
                content,
                new TypeReference<ApiResponse<DiseaseSpectrumDTO>>() {}
        ).getData();

        // Verify response
        assertEquals("TestSpectrum", responseSpectrum.getName());
        assertEquals(organName, responseSpectrum.getOrganName());
    }

    private DiseaseSpectrumDTO createTestDiseaseSpectrum(String name, String organName) {
        DiseaseSpectrumDTO dto = new DiseaseSpectrumDTO();
        dto.setName(name);
        dto.setOrganName(organName);
        return testUtil.createDiseaseSpectrum(dto);
    }
} 
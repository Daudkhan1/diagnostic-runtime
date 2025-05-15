package app.api.diagnosticruntime.patient.controller;

import app.api.diagnosticruntime.config.MongoDBTestContainer;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.util.AESUtil;
import app.api.diagnosticruntime.util.TestUtil;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
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
public class PatientControllerTest {

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
    @WithMockUser(username = "pathologist@example.com", roles = {"USER"})
    void shouldGetPatientByMrn() throws Exception {
        // 1. Create a patient with raw MRN (TestUtil should handle encryption)
        String rawMrn = "1123412341234";
        Patient patient = testUtil.createPatient(rawMrn);

        // 2. Test successful retrieval using raw MRN
        mockMvc.perform(get("/api/patient/mrn/{id}", rawMrn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(patient.getId()))
                // Don't directly compare encrypted values
                .andExpect(jsonPath("$.mrn").value(AESUtil.encrypt(rawMrn)))  // Should get decrypted value
                .andExpect(jsonPath("$.gender").value(patient.getGender()))
                .andExpect(jsonPath("$.age").value(patient.getAge()))
                .andExpect(jsonPath("$.praidId").value(1));

        // 3. Test non-existent MRN
        mockMvc.perform(get("/api/patient/mrn/{id}", "nonexistent"))
                .andExpect(status().isNotFound());
    }
} 
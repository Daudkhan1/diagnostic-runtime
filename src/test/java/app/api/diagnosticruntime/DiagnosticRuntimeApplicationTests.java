package app.api.diagnosticruntime;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@ActiveProfiles({"test", "without-ai-annotation"})
@TestPropertySource(locations = "classpath:application-test.properties")
class DiagnosticRuntimeApplicationTests {

    @Test
    void contextLoads() {
    }

}

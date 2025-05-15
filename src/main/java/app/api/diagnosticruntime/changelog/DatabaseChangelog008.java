package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Random;

@ChangeUnit(id = "addNationalPatientId", order = "008", author = "zaid", transactional = true)
public class DatabaseChangelog008 {
    @Execution
    public void addNationalPatientId(MongoTemplate mongoTemplate) {
        List<Document> patients = mongoTemplate.find(new Query(), Document.class, "patients");

        Random random = new Random();

        for (Document patient : patients) {
            String nationalPatientId = generateRandomNationalId(random);
            patient.put("national_patient_id", nationalPatientId); // Add new field
            mongoTemplate.save(patient, "patients"); // Save updated patient
        }
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // Remove the "national_patient_id" field in case of rollback
        mongoTemplate.updateMulti(
                new Query(),
                new Update().unset("national_patient_id"),
                "patients"
        );
    }

    private String generateRandomNationalId(Random random) {
        StringBuilder nationalId = new StringBuilder();
        for (int i = 0; i < 13; i++) {
            nationalId.append(random.nextInt(10)); // Append random digits
        }
        return nationalId.toString();
    }
}

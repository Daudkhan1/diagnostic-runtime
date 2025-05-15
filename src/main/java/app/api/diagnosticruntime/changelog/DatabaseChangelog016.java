package app.api.diagnosticruntime.changelog;

import com.mongodb.BasicDBObject;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "splitNameOfPatientToFirstAndLastName", order = "016", author = "zaid", transactional = true)
public class DatabaseChangelog016 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("patients").find().forEach(document -> {
            String name = document.getString("name");
            if (name != null && !name.isBlank()) {
                String[] parts = name.split(" ", 2); // Split name into two parts
                String firstName = parts[0];
                String lastName = parts.length > 1 ? parts[1] : ""; // Handle single-word names
                document.put("first_name", firstName);
                document.put("last_name", lastName);
                document.remove("name");
                mongoTemplate.getCollection("patients").replaceOne(
                        new BasicDBObject("_id", document.get("_id")),
                        document
                );
            }
        });
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("patients").find().forEach(document -> {
            String firstName = document.getString("first_name");
            String lastName = document.getString("last_name");

            // Merge firstName and lastName back into name
            String name = firstName;
            if (lastName != null && !lastName.isBlank()) {
                name += " " + lastName;
            }

            document.put("name", name.trim());
            document.remove("first_name");
            document.remove("last_name");

            mongoTemplate.getCollection("patients").replaceOne(
                    new BasicDBObject("_id", document.get("_id")),
                    document
            );
        });
    }
}

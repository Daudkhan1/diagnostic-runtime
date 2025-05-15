package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "migrateAgeToNumeric", order = "032", author = "assistant", transactional = false)
public class DatabaseChangelog032 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Get all patients
        List<Document> patients = mongoTemplate.getCollection("patients").find().into(new ArrayList<>());
        
        for (Document patient : patients) {
            String stringAge = patient.getString("age");
            if (stringAge != null) {
                try {
                    // Parse the string age to integer
                    int numericAge = Integer.parseInt(stringAge.trim());
                    
                    // Update the document with new numeric age field
                    Document update = new Document("$set", new Document("numeric_age", numericAge));
                    mongoTemplate.getCollection("patients").updateOne(
                        new Document("_id", patient.get("_id")),
                        update
                    );
                } catch (NumberFormatException e) {
                    // Log error for invalid age values
                    System.err.println("Invalid age value for patient " + patient.get("_id") + ": " + stringAge);
                }
            }
        }

        // Rename the field from numeric_age to age (this will remove the old age field)
        Document rename = new Document("$rename", new Document("numeric_age", "age"));
        mongoTemplate.getCollection("patients").updateMany(new Document(), rename);
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Get all patients
        List<Document> patients = mongoTemplate.getCollection("patients").find().into(new ArrayList<>());
        
        for (Document patient : patients) {
            Integer numericAge = patient.getInteger("age");
            if (numericAge != null) {
                // Convert numeric age back to string
                Document update = new Document("$set", new Document("string_age", numericAge.toString()));
                mongoTemplate.getCollection("patients").updateOne(
                    new Document("_id", patient.get("_id")),
                    update
                );
            }
        }

        // Rename the field from string_age to age
        Document rename = new Document("$rename", new Document("string_age", "age"));
        mongoTemplate.getCollection("patients").updateMany(new Document(), rename);
    }
} 
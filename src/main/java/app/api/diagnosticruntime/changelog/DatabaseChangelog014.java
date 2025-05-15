package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "addSoftDeleteToPatientSlides", order = "014", author = "zaid", transactional = true)
public class DatabaseChangelog014 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Step 1: Add `isDeleted` field with a default value of `false` to all patient slides
        mongoTemplate.getCollection("patient_slides").updateMany(
                new Document(),
                new Document("$set", new Document("is_deleted", false))
        );
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Remove the `isDeleted` field in case of rollback
        mongoTemplate.getCollection("patient_slides")
                .updateMany(
                        new Document(),
                        new Document("$unset", new Document("is_deleted", ""))
                );
    }
}

package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "addSoftDeleteToCases", order = "015", author = "zaid", transactional = true)
public class DatabaseChangelog015 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("cases").updateMany(
                new Document(),
                new Document("$set", new Document("is_deleted", false))
        );
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("cases").updateMany(
                new Document(),
                new Document("$unset", new Document("is_deleted", ""))
        );
    }
}

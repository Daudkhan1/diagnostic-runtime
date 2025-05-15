package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

@ChangeUnit(id = "addAnonymizerEnabledFlag", order = "029", author = "zaid", transactional = true)
public class DatabaseChangelog029 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        Document configDocument = new Document();
        configDocument.put("key", "anonymizer"); // Use _id as the key
        configDocument.put("value", "false"); // Initial value

        mongoTemplate.getCollection("config").insertOne(configDocument);
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("config").deleteOne(new Document("key", "anonymizer"));
    }
}

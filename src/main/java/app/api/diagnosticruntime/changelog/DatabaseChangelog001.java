package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "addStatusField", order = "001", author = "zaid", transactional = true)
public class DatabaseChangelog001 {

    @Execution
    public void addStatusField(MongoTemplate mongoTemplate) {
        // Update all documents by setting "status" field to UserStatus.ACTIVE if it doesn't exist
        mongoTemplate.updateMulti(
                new Query(),
                new Update().set("status", "ACTIVE"), // Store enum name in MongoDB
                "users"
        );
    }

    @RollbackExecution
    public void rollbackStatusField(MongoTemplate mongoTemplate) {
        mongoTemplate.updateMulti(
                new Query(),
                new Update().unset("status"),
                "users"
        );
    }
}

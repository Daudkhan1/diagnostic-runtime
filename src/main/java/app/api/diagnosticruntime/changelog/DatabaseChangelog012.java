package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "addSoftDeleteAndLastModifiedUser", order = "012", author = "zaid", transactional = true)
public class DatabaseChangelog012 {

    @Execution
    public void execute(MongoTemplate mongoTemplate) {
        // Add 'is_deleted' field and 'last_modified_user' to all existing annotations
        mongoTemplate.updateMulti(
                new Query(),
                new Update()
                        .set("is_deleted", false) // Default to not deleted
                        .set("last_modified_user", "SYSTEM"), // Default to SYSTEM as the user
                "annotations"
        );
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // Remove 'is_deleted' and 'last_modified_user' fields in case of rollback
        mongoTemplate.updateMulti(
                new Query(),
                new Update()
                        .unset("is_deleted")
                        .unset("last_modified_user"),
                "annotations"
        );
    }
}

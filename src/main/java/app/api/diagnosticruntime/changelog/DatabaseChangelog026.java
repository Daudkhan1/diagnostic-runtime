package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "setDefaultAnnotationState", order = "026", author = "user", transactional = false)
public class DatabaseChangelog026 {

    private static final String ANNOTATIONS_COLLECTION = "annotations";

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Find all annotations where state is null
        Query query = new Query(
                Criteria.where("state").isNull()
        );

        // Update to set state to ACCEPTED
        Update update = new Update().set("state", "ACCEPTED");

        // Update all matching documents
        mongoTemplate.updateMulti(
                query,
                update,
                ANNOTATIONS_COLLECTION
        );
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Find all annotations where state is ACCEPTED
        Query query = new Query(
                Criteria.where("state").is("ACCEPTED")
        );

        // Remove the state field
        Update update = new Update().unset("state");

        // Update all matching documents
        mongoTemplate.updateMulti(
                query,
                update,
                ANNOTATIONS_COLLECTION
        );
    }
}

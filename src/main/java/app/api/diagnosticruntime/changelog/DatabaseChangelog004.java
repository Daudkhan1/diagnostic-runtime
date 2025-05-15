package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "updateCaseStatusToEnum", order = "004", author = "zaid", transactional = true)
public class DatabaseChangelog004 {

    @Execution
    public void updateCaseStatus(MongoTemplate mongoTemplate) {
        // Update all "New" statuses to "NEW"
        mongoTemplate.updateMulti(
                new Query(Criteria.where("Status").is("New")),
                new Update().set("Status", "NEW"),
                "cases"
        );
    }

    @RollbackExecution
    public void rollbackCaseStatus(MongoTemplate mongoTemplate) {
        // Rollback "NEW" statuses back to "New"
        mongoTemplate.updateMulti(
                new Query(Criteria.where("Status").is("NEW")),
                new Update().set("Status", "New"),
                "cases"
        );
    }
}


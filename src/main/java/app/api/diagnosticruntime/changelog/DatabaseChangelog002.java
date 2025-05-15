package app.api.diagnosticruntime.changelog;


import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "updateUserRoleToEnum", order = "002", author = "zaid", transactional = true)
public class DatabaseChangelog002 {

    @Execution
    public void updateUserRoles(MongoTemplate mongoTemplate) {
        // Update "radiologist" to "SENIOR_RADIOLOGIST"
        mongoTemplate.updateMulti(
                new Query(Criteria.where("role").is("radiologist")),
                new Update().set("role", "SENIOR_RADIOLOGIST"),
                "users"
        );

        // Update "pathologist" to "SENIOR_PATHOLOGIST"
        mongoTemplate.updateMulti(
                new Query(Criteria.where("role").is("pathologist")),
                new Update().set("role", "SENIOR_PATHOLOGIST"),
                "users"
        );

        // Update "admin" to "ADMIN"
        mongoTemplate.updateMulti(
                new Query(Criteria.where("role").is("admin")),
                new Update().set("role", "ADMIN"),
                "users"
        );

        // Delete the 'roles' collection as it's no longer needed
//        mongoTemplate.dropCollection("roles");
    }

    @RollbackExecution
    public void rollbackUserRoles(MongoTemplate mongoTemplate) {
        // Rollback each enum role back to its original string value

        mongoTemplate.updateMulti(
                new Query(Criteria.where("role").is("SENIOR_RADIOLOGIST")),
                new Update().set("role", "radiologist"),
                "users"
        );

        mongoTemplate.updateMulti(
                new Query(Criteria.where("role").is("SENIOR_PATHOLOGIST")),
                new Update().set("role", "pathologist"),
                "users"
        );

        mongoTemplate.updateMulti(
                new Query(Criteria.where("role").is("ADMIN")),
                new Update().set("role", "admin"),
                "users"
        );
    }
}


package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@ChangeUnit(id = "simplifyUserRoles", order = "025", author = "user", transactional = false)
public class DatabaseChangelog025 {

    private static final String USERS_COLLECTION = "users";
    private static final Map<String, String> ROLE_MAPPINGS = new HashMap<>();
    private static final Map<String, String> REVERSE_ROLE_MAPPINGS = new HashMap<>();

    static {
        // Define role mappings
        ROLE_MAPPINGS.put("SENIOR_RADIOLOGIST", "RADIOLOGIST");
        ROLE_MAPPINGS.put("JUNIOR_RADIOLOGIST", "RADIOLOGIST");
        ROLE_MAPPINGS.put("SENIOR_PATHOLOGIST", "PATHOLOGIST");
        ROLE_MAPPINGS.put("JUNIOR_PATHOLOGIST", "PATHOLOGIST");

        // Define reverse mappings for rollback
        // We'll map to senior roles during rollback as a default
        REVERSE_ROLE_MAPPINGS.put("RADIOLOGIST", "SENIOR_RADIOLOGIST");
        REVERSE_ROLE_MAPPINGS.put("PATHOLOGIST", "SENIOR_PATHOLOGIST");
    }

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Get all users with old roles
        Query query = new Query(Criteria.where("role").in(ROLE_MAPPINGS.keySet()));
        List<Document> usersToUpdate = mongoTemplate.find(query, Document.class, USERS_COLLECTION);

        for (Document user : usersToUpdate) {
            String currentRole = user.getString("role");
            String newRole = ROLE_MAPPINGS.get(currentRole);

            if (newRole != null) {
                Query updateQuery = new Query(Criteria.where("_id").is(user.get("_id")));
                Update update = new Update().set("role", newRole);
                mongoTemplate.updateFirst(updateQuery, update, USERS_COLLECTION);
            }
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Get all users with new roles
        Query query = new Query(Criteria.where("role").in(REVERSE_ROLE_MAPPINGS.keySet()));
        List<Document> usersToUpdate = mongoTemplate.find(query, Document.class, USERS_COLLECTION);

        for (Document user : usersToUpdate) {
            String currentRole = user.getString("role");
            String oldRole = REVERSE_ROLE_MAPPINGS.get(currentRole);

            if (oldRole != null) {
                Query updateQuery = new Query(Criteria.where("_id").is(user.get("_id")));
                Update update = new Update().set("role", oldRole);
                mongoTemplate.updateFirst(updateQuery, update, USERS_COLLECTION);
            }
        }
    }
}

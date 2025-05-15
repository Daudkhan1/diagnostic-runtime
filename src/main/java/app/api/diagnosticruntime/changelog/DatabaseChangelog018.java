package app.api.diagnosticruntime.changelog;


import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;

@ChangeUnit(id = "addDefaultAdminUser", order = "018", author = "zaid", transactional = true)
public class DatabaseChangelog018 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        String email = "test@example.com"; // Unique email to check
        Query query = new Query();
        query.addCriteria(Criteria.where("email").is(email));

        boolean userExists = mongoTemplate.exists(query, "users");

        if (!userExists) {
            Document userDocument = new Document();
            userDocument.put("fullname", "test");
            userDocument.put("email", email);
            userDocument.put("password", "$2a$10$FI8sn.UGMrpXR/9eUO33AuwvY6achVpMivmOHYgceCjijC5EPUIWu"); // Bcrypt hashed password
            userDocument.put("status", "ACTIVE");
            userDocument.put("phone_number", "+92111000666");
            userDocument.put("registration_date", LocalDate.now());
            userDocument.put("role", "ADMIN");
            userDocument.put("_class", "app.api.diagnosticruntime.userdetails.model.User");

            mongoTemplate.getCollection("users").insertOne(userDocument);
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        throw new UnsupportedOperationException("Rollback not supported for this migration.");
    }
}

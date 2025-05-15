package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

@ChangeUnit(id = "setRandomRegistrationDates", order = "003", author = "zaid", transactional = true)
public class DatabaseChangelog003 {

    @Execution
    public void setRandomRegistrationDates(MongoTemplate mongoTemplate) {
        // Fetch all user documents
        List<Document> userDocuments = mongoTemplate.getCollection("users")
                .find()
                .into(new java.util.ArrayList<>());

        Random random = new Random();
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);

        for (Document userDocument : userDocuments) {
            String userId = userDocument.getObjectId("_id").toString();

            // Generate a random date between last month and today
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastMonth, today);
            LocalDate randomRegistrationDate = lastMonth.plusDays(random.nextInt((int) daysBetween + 1));

            // Update the user with the random registration date
            mongoTemplate.getCollection("users").updateOne(
                    new Document("_id", userDocument.getObjectId("_id")),
                    new Document("$set", new Document("registration_date", randomRegistrationDate.toString()))
            );
        }
    }

    @RollbackExecution
    public void rollbackRegistrationDates(MongoTemplate mongoTemplate) {
        // Remove the registration_date field from all users in case of rollback
        mongoTemplate.updateMulti(
                new Query(),
                new Update().unset("registration_date"),
                "users"
        );
    }
}


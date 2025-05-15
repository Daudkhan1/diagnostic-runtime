package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
@ChangeUnit(id = "migrateCommentsToNewCollection", order = "009", author = "zaid", transactional = true)
public class DatabaseChangelog009 {

    @Execution
    public void migrateComments(MongoTemplate mongoTemplate) {
        // Step 1: Fetch all patients with the "comments" field
        List<Document> patientDocuments = mongoTemplate.getCollection("patients")
                .find(new Document("comments", new Document("$exists", true)))
                .into(new ArrayList<>());

        // Step 2: Process each patient document
        for (Document patientDocument : patientDocuments) {
            ObjectId patientObjectId = patientDocument.getObjectId("_id");
            String patientId = patientObjectId.toString();
            String comments = patientDocument.getString("comments");

            if (comments != null && !comments.isEmpty()) {
                // Fetch all cases linked to this patient
                List<Document> cases = mongoTemplate.find(
                        new Query(Criteria.where("patient_id").is(patientId)),
                        Document.class,
                        "cases"
                );

                for (Document caseItem : cases) {
                    // Create a new Comment document
                    Document commentDocument = new Document();
                    commentDocument.put("case_id", caseItem.getObjectId("_id").toString());
                    commentDocument.put("comment_text", comments);
                    commentDocument.put("creation_date", LocalDate.now().toString()); // Store as ISO string
                    commentDocument.put("creation_user", "SYSTEM");
                    commentDocument.put("modificationDate", LocalDate.now().toString());
                    commentDocument.put("modificationUser", "SYSTEM");

                    // Save the comment document to the "comments" collection
                    mongoTemplate.getCollection("comments").insertOne(commentDocument);
                }
            }
        }

        // Step 3: Remove the "comments" field from the patients collection
        mongoTemplate.getCollection("patients")
                .updateMany(
                        new Document("comments", new Document("$exists", true)),
                        new Document("$unset", new Document("comments", ""))
                );
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        throw new UnsupportedOperationException("Rollback not supported for this migration.");
    }
}

package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "updatePatientSlidesCaseRelation", order = "010", author = "zaid", transactional = true)
public class DatabaseChangelog010 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Step 1: Fetch all cases
        List<Document> cases = mongoTemplate.getCollection("cases")
                .find()
                .into(new ArrayList<>());

        for (Document caseDocument : cases) {
            String caseId = caseDocument.getObjectId("_id").toString();

            // Step 2: Get patient_slide_ids from the case
            List<String> patientSlideIds = caseDocument.getList("patient_slide_ids", String.class);

            if (patientSlideIds != null && !patientSlideIds.isEmpty()) {
                // Step 3: Update each patient slide with the corresponding case ID
                for (String slideId : patientSlideIds) {
                    mongoTemplate.getCollection("patient_slides")
                            .updateOne(
                                    new Document("_id", new org.bson.types.ObjectId(slideId)),
                                    new Document("$set", new Document("case_id", caseId))
                            );
                }
            }
        }

        // Step 4: Remove patient_slide_ids field from cases
        mongoTemplate.getCollection("cases")
                .updateMany(
                        new Document("patient_slide_ids", new Document("$exists", true)),
                        new Document("$unset", new Document("patient_slide_ids", ""))
                );
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        throw new UnsupportedOperationException("Rollback is not supported for this migration.");
    }
}

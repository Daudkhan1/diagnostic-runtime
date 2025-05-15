package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "splitCaseToNewCollections", order = "006", author = "zaid", transactional = true)
public class DatabaseChangelog006 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Fetch all case documents
        List<Document> cases = mongoTemplate.getCollection("cases").find().into(new ArrayList<>());

        for (Document caseDoc : cases) {
            String caseId = caseDoc.getObjectId("_id").toString();

            // Migrate patient details to the "patients" collection
            String patientId = null;
            if (caseDoc.containsKey("patient_details")) {
                Document patientDetails = (Document) caseDoc.get("patient_details");

                // Save patient details as a new patient document
                Document patient = new Document()
                        .append("name", patientDetails.getString("name"))
                        .append("gender", patientDetails.getString("gender"))
                        .append("age", patientDetails.getString("age"))
                        .append("diagnosis", patientDetails.getString("diagnosis"))
                        .append("history", patientDetails.getString("history"))
                        .append("description", patientDetails.getString("description"))
                        .append("comments", patientDetails.getString("comments"));
                mongoTemplate.getCollection("patients").insertOne(patient);

                patientId = patient.getObjectId("_id").toString();
            }

            // Migrate patient slides to the "patient_slides" collection
            List<String> slideIds = new ArrayList<>();
            if (caseDoc.containsKey("patient_slides")) {
                List<Document> slides = (List<Document>) caseDoc.get("patient_slides");

                for (Document slide : slides) {
                    mongoTemplate.getCollection("patient_slides").insertOne(slide);
                    slideIds.add(slide.getObjectId("_id").toString());
                }
            }

            // Update the case document with references to the new collections
            Update update = new Update()
                    .set("patient_id", patientId)
                    .set("patient_slide_ids", slideIds)
                    .unset("patient_details") // Remove the embedded PatientDetails field
                    .unset("patient_slides"); // Remove the embedded PatientSlides field
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(caseId)), update, "cases");
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Fetch all cases to restore the original state
        List<Document> cases = mongoTemplate.getCollection("cases").find().into(new ArrayList<>());

        for (Document caseDoc : cases) {
            String caseId = caseDoc.getObjectId("_id").toString();

            // Restore patient details from the "patients" collection
            if (caseDoc.containsKey("patient_id")) {
                String patientId = caseDoc.getString("patient_id");
                Document patient = mongoTemplate.getCollection("patients").find(new Document("_id", patientId)).first();

                if (patient != null) {
                    Document patientDetails = new Document()
                            .append("name", patient.getString("name"))
                            .append("gender", patient.getString("gender"))
                            .append("age", patient.getString("age"))
                            .append("diagnosis", patient.getString("diagnosis"))
                            .append("history", patient.getString("history"))
                            .append("description", patient.getString("description"))
                            .append("comments", patient.getString("comments"));

                    Update update = new Update().set("patient_details", patientDetails);
                    mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(caseId)), update, "cases");
                    mongoTemplate.getCollection("patients").deleteOne(new Document("_id", patientId));
                }
            }

            // Restore patient slides from the "patient_slides" collection
            if (caseDoc.containsKey("patient_slide_ids")) {
                List<String> slideIds = (List<String>) caseDoc.get("patient_slide_ids");
                List<Document> slides = mongoTemplate.getCollection("patient_slides")
                        .find(new Document("_id", new Document("$in", slideIds)))
                        .into(new ArrayList<>());

                if (!slides.isEmpty()) {
                    Update update = new Update().set("patient_slides", slides);
                    mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(caseId)), update, "cases");
                    mongoTemplate.getCollection("patient_slides").deleteMany(new Document("_id", new Document("$in", slideIds)));
                }
            }

            // Remove references to the new collections from the case document
            Update update = new Update()
                    .unset("patient_id")
                    .unset("patient_slide_ids");
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(caseId)), update, "cases");
        }
    }
}

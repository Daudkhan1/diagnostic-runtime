package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.MongoTemplate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ChangeUnit(id = "updatePatientSlideOrgan", order = "022", author = "zaid", transactional = false)
public class DatabaseChangelog022 {

    private static final String PATIENT_SLIDES_COLLECTION = "patient_slides";
    private static final String CASES_COLLECTION = "cases";
    private static final String ORGAN_FIELD = "organ";
    private static final String CASE_ID_FIELD = "case_id";

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Fetch all cases with a non-null organ field
        List<Document> cases = mongoTemplate.find(
                new Query(Criteria.where(ORGAN_FIELD).ne(null)), Document.class, CASES_COLLECTION
        );

        // Create a map of caseId -> organ
        Map<Object, String> caseOrganMap = cases.stream()
                .collect(Collectors.toMap(
                        c -> c.getObjectId("_id"),  // Case ID
                        c -> c.getString(ORGAN_FIELD)  // Organ
                ));

        // Iterate over patient_slides and update the missing organ field
        for (Map.Entry<Object, String> entry : caseOrganMap.entrySet()) {
            Query query = new Query(
                    Criteria.where(CASE_ID_FIELD).is(entry.getKey().toString())
                            .and(ORGAN_FIELD).is(null)  // Only update if organ is null
            );
            Update update = new Update().set(ORGAN_FIELD, entry.getValue());

            mongoTemplate.updateMulti(query, update, PATIENT_SLIDES_COLLECTION);
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        Query query = new Query(Criteria.where(ORGAN_FIELD).ne(null));
        Update update = new Update().set(ORGAN_FIELD, null);

        mongoTemplate.updateMulti(query, update, PATIENT_SLIDES_COLLECTION);
    }
}

package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.bson.Document;
import java.util.List;

@ChangeUnit(id = "addCreatedByToAnnotations", order = "024", author = "user", transactional = false)
public class DatabaseChangelog024 {

    private static final String ANNOTATIONS_COLLECTION = "annotations";

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Get all annotations
        List<Document> annotations = mongoTemplate.findAll(Document.class, ANNOTATIONS_COLLECTION);
        
        for (Document annotation : annotations) {
            String lastModifiedUser = annotation.getString("last_modified_user");
            
            // Update only if lastModifiedUser exists and createdBy doesn't
            if (lastModifiedUser != null && !annotation.containsKey("created_by")) {
                mongoTemplate.update(Document.class)
                    .inCollection(ANNOTATIONS_COLLECTION)
                    .matching(new Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(annotation.get("_id"))))
                    .apply(new Update().set("created_by", lastModifiedUser))
                    .first();
            }
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Remove created_by field from all documents
        mongoTemplate.updateMulti(
            new Query(),
            new Update().unset("created_by"),
            ANNOTATIONS_COLLECTION
        );
    }
} 
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ChangeUnit(id = "capitalizeOrganNames", order = "030", author = "zaid", transactional = false)
public class DatabaseChangelog030 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Find all organs
        List<Document> organs = mongoTemplate.getCollection("organs").find().into(new ArrayList<>());
        
        // Map to track potential duplicates
        Map<String, List<Document>> nameGroups = new HashMap<>();
        
        // Group organs by their uppercase names
        for (Document organ : organs) {
            String currentName = organ.getString("name");
            if (currentName != null && !currentName.isEmpty()) {
                String uppercaseName = currentName.toUpperCase();
                nameGroups.computeIfAbsent(uppercaseName, k -> new ArrayList<>()).add(organ);
            }
        }
        
        // Process each group
        for (Map.Entry<String, List<Document>> entry : nameGroups.entrySet()) {
            List<Document> group = entry.getValue();
            String newName = entry.getKey();
            
            if (group.size() == 1) {
                // Single organ - just update the name
                Document organ = group.get(0);
                Query query = new Query(Criteria.where("_id").is(organ.getObjectId("_id")));
                Update update = new Update().set("name", newName);
                mongoTemplate.updateFirst(query, update, "organs");
            } else {
                // Multiple organs with same uppercase name - append numbers
                for (int i = 0; i < group.size(); i++) {
                    Document organ = group.get(i);
                    String numberedName = newName + "_" + (i + 1);
                    Query query = new Query(Criteria.where("_id").is(organ.getObjectId("_id")));
                    Update update = new Update().set("name", numberedName);
                    mongoTemplate.updateFirst(query, update, "organs");
                }
            }
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Note: Rollback would require storing original names, which we don't have
        // This is a limitation of this migration
        // In a production environment, you might want to store the original names
        // in a temporary collection before making changes
    }
} 
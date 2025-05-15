package app.api.diagnosticruntime.changelog;

import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;

@ChangeUnit(id = "createMissingCaseHistory", order = "023", author = "user", transactional = false)
public class DatabaseChangelog023 {

    private static final String CASES_COLLECTION = "cases";
    private static final String HISTORY_COLLECTION = "case_history";
    private static final String USERS_COLLECTION = "users";

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        // Get all cases
        List<Document> cases = mongoTemplate.findAll(Document.class, CASES_COLLECTION);
        
        // Get senior pathologists and radiologists
        List<Document> seniorPathologists = mongoTemplate.find(
            Query.query(Criteria.where("role").is("SENIOR_PATHOLOGIST")),
            Document.class,
            USERS_COLLECTION
        );
        
        List<Document> seniorRadiologists = mongoTemplate.find(
            Query.query(Criteria.where("role").is("SENIOR_RADIOLOGIST")),
            Document.class,
            USERS_COLLECTION
        );

        for (Document caseDoc : cases) {
            String caseId = caseDoc.getObjectId("_id").toString();
            
            // Check if case already has history
            long historyCount = mongoTemplate.count(
                Query.query(Criteria.where("case_id").is(caseId)),
                HISTORY_COLLECTION
            );
            
            if (historyCount == 0) {
                String caseType = caseDoc.getString("type");
                String status = caseDoc.getString("status");
                
                // Select appropriate senior user based on case type
                List<Document> seniorUsers = CaseType.PATHOLOGY.toString().equals(caseType) 
                    ? seniorPathologists 
                    : seniorRadiologists;
                
                if (!seniorUsers.isEmpty()) {
                    Document seniorUser = seniorUsers.get(0);
                    String seniorUserId = seniorUser.getObjectId("_id").toString();
                    
                    List<Document> historyEntries = new ArrayList<>();
                    
                    // Create NEW status history
                    Document newHistory = new Document()
                        .append("_id", new ObjectId())
                        .append("case_id", caseId)
                        .append("previous_status", null)
                        .append("new_status", CaseStatus.NEW.toString())
                        .append("action_by_pathologist_id", null)
                        .append("transferred_to_pathologist_id", null)
                        .append("created_at", Instant.now())
                        .append("note", "Initial case creation");
                    historyEntries.add(newHistory);
                    
                    // If case is not NEW, add appropriate history entries
                    if (!CaseStatus.NEW.toString().equals(status)) {
                        // Add IN_PROGRESS entry
                        Document inProgressHistory = new Document()
                            .append("_id", new ObjectId())
                            .append("case_id", caseId)
                            .append("previous_status", CaseStatus.NEW.toString())
                            .append("new_status", CaseStatus.IN_PROGRESS.toString())
                            .append("action_by_pathologist_id", seniorUserId)
                            .append("transferred_to_pathologist_id", null)
                            .append("created_at", Instant.now().plusSeconds(1))
                            .append("note", "Case assigned");
                        historyEntries.add(inProgressHistory);
                        
                        if (CaseStatus.COMPLETE.toString().equals(status)) {
                            // Add REFERRED entry
                            Document referredHistory = new Document()
                                .append("_id", new ObjectId())
                                .append("case_id", caseId)
                                .append("previous_status", CaseStatus.IN_PROGRESS.toString())
                                .append("new_status", CaseStatus.REFERRED.toString())
                                .append("action_by_pathologist_id", seniorUserId)
                                .append("transferred_to_pathologist_id", seniorUserId)
                                .append("created_at", Instant.now().plusSeconds(2))
                                .append("note", "Case referred for completion");
                            historyEntries.add(referredHistory);
                            
                            // Add COMPLETE entry
                            Document completeHistory = new Document()
                                .append("_id", new ObjectId())
                                .append("case_id", caseId)
                                .append("previous_status", CaseStatus.REFERRED.toString())
                                .append("new_status", CaseStatus.COMPLETE.toString())
                                .append("action_by_pathologist_id", seniorUserId)
                                .append("transferred_to_pathologist_id", seniorUserId)
                                .append("created_at", Instant.now().plusSeconds(3))
                                .append("note", "Case completed");
                            historyEntries.add(completeHistory);
                        } else if (CaseStatus.REFERRED.toString().equals(status)) {
                            // Add REFERRED entry
                            Document referredHistory = new Document()
                                .append("_id", new ObjectId())
                                .append("case_id", caseId)
                                .append("previous_status", CaseStatus.IN_PROGRESS.toString())
                                .append("new_status", CaseStatus.REFERRED.toString())
                                .append("action_by_pathologist_id", seniorUserId)
                                .append("transferred_to_pathologist_id", seniorUserId)
                                .append("created_at", Instant.now().plusSeconds(2))
                                .append("note", "Case referred");
                            historyEntries.add(referredHistory);
                        }
                    }
                    
                    // Insert all history entries
                    for (Document historyEntry : historyEntries) {
                        mongoTemplate.insert(historyEntry, HISTORY_COLLECTION);
                    }
                }
            }
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Get all cases
        List<Document> cases = mongoTemplate.findAll(Document.class, CASES_COLLECTION);
        
        for (Document caseDoc : cases) {
            String caseId = caseDoc.getObjectId("_id").toString();
            
            // Find history entries for this case
            List<Document> historyEntries = mongoTemplate.find(
                Query.query(Criteria.where("case_id").is(caseId)),
                Document.class,
                HISTORY_COLLECTION
            );
            
            // If case has exactly the number of entries we created based on its status,
            // and the notes match our migration's notes, remove them
            String status = caseDoc.getString("status");
            int expectedEntries = getExpectedEntryCount(status);
            
            if (historyEntries.size() == expectedEntries) {
                boolean createdByMigration = historyEntries.stream()
                    .allMatch(entry -> isEntryFromMigration(entry));
                
                if (createdByMigration) {
                    mongoTemplate.remove(
                        Query.query(Criteria.where("case_id").is(caseId)),
                        HISTORY_COLLECTION
                    );
                }
            }
        }
    }
    
    private int getExpectedEntryCount(String status) {
        if (CaseStatus.NEW.toString().equals(status)) {
            return 1; // Only NEW entry
        } else if (CaseStatus.IN_PROGRESS.toString().equals(status)) {
            return 2; // NEW + IN_PROGRESS
        } else if (CaseStatus.REFERRED.toString().equals(status)) {
            return 3; // NEW + IN_PROGRESS + REFERRED
        } else if (CaseStatus.COMPLETE.toString().equals(status)) {
            return 4; // NEW + IN_PROGRESS + REFERRED + COMPLETE
        }
        return 0;
    }
    
    private boolean isEntryFromMigration(Document entry) {
        String note = entry.getString("note");
        return "Initial case creation".equals(note) ||
               "Case assigned".equals(note) ||
               "Case referred".equals(note) ||
               "Case referred for completion".equals(note) ||
               "Case completed".equals(note);
    }
} 
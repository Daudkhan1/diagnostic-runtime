package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.IndexOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChangeUnit(id = "add-praid-to-patients", order = "027")
public class DatabaseChangelog027 {
    private static final Logger log = LoggerFactory.getLogger(DatabaseChangelog027.class);

    @Execution
    public void addPraidToExistingPatients(MongoDatabase db) {
        try {
            log.info("Starting migration to add PRAID to existing patients");
            MongoCollection<Document> patientsCollection = db.getCollection("patients");

            // Get all patients without praidId and sort by _id to maintain order
            var patients = patientsCollection
                    .find(new Document("praid", new Document("$exists", false)))
                    .sort(new Document("_id", 1))
                    .into(new java.util.ArrayList<>());

            log.info("Found {} patients without PRAID", patients.size());

            long counter = 1;
            for (Document patient : patients) {
                patientsCollection.updateOne(
                        new Document("_id", patient.get("_id")),
                        Updates.set("praid", counter++)
                );
            }

            // Create unique index with proper IndexOptions
            IndexOptions indexOptions = new IndexOptions().unique(true);
            patientsCollection.createIndex(new Document("praid", 1), indexOptions);

            log.info("Successfully added PRAID to {} patients", patients.size());
        } catch (Exception e) {
            log.error("Error while adding PRAID to patients", e);
            throw e;
        }
    }

    @RollbackExecution
    public void rollback(MongoDatabase db) {
        MongoCollection<Document> patientsCollection = db.getCollection("patients");

        // Drop the praid index
        patientsCollection.dropIndex("praid_1");

        // Remove praid field from all documents
        patientsCollection.updateMany(new Document(), Updates.unset("praid"));
    }
}
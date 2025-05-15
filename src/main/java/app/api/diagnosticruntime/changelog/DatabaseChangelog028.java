package app.api.diagnosticruntime.changelog;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@ChangeUnit(id = "migrate-organ-data", order = "028")
public class DatabaseChangelog028 {
    private static final Logger log = LoggerFactory.getLogger(DatabaseChangelog028.class);

    @Execution
    public void migrateOrganData(MongoDatabase db) {
        try {
            log.info("Starting organ data migration");
            MongoCollection<Document> slidesCollection = db.getCollection("patient_slides");
            MongoCollection<Document> organsCollection = db.getCollection("organs");

            // Create unique index on organ name
            organsCollection.createIndex(
                    new Document("name", 1),
                    new IndexOptions().unique(true)
            );

            // Get all distinct organ values from patient_slides
            List<String> organNames = slidesCollection
                    .distinct("organ", String.class)
                    .into(new ArrayList<>());

            log.info("Found {} distinct organ names", organNames.size());

            // Process each organ name
            for (String organName : organNames) {
                if (organName == null || organName.isEmpty()) {
                    continue;
                }

                // Insert organ document
                Document organDoc = new Document("name", organName);
                organsCollection.insertOne(organDoc);
                String organId = organDoc.getObjectId("_id").toString();

                // Update slides with new organ_id
                slidesCollection.updateMany(
                        new Document("organ", organName),
                        new Document("$set", new Document("organ_id", organId))
                );

                log.info("Processed organ: {}", organName);
            }

            // Remove old organ field
            slidesCollection.updateMany(
                    new Document(),
                    new Document("$unset", new Document("organ", ""))
            );

            log.info("Successfully completed organ data migration");
        } catch (Exception e) {
            log.error("Error during organ data migration", e);
            throw e;
        }
    }

    @RollbackExecution
    public void rollback(MongoDatabase db) {
        try {
            log.info("Starting organ data rollback");
            MongoCollection<Document> slidesCollection = db.getCollection("patient_slides");
            MongoCollection<Document> organsCollection = db.getCollection("organs");

            // Get all organs
            List<Document> organs = organsCollection
                    .find()
                    .into(new ArrayList<>());

            // Restore organ names to slides
            for (Document organ : organs) {
                slidesCollection.updateMany(
                        new Document("organ_id", organ.getObjectId("_id").toString()),
                        new Document("$set", new Document("organ", organ.getString("name")))
                );
            }

            // Remove organ_id field
            slidesCollection.updateMany(
                    new Document(),
                    new Document("$unset", new Document("organ_id", ""))
            );

            // Drop organs collection
            organsCollection.drop();

            log.info("Successfully completed organ data rollback");
        } catch (Exception e) {
            log.error("Error during organ data rollback", e);
            throw e;
        }
    }
}

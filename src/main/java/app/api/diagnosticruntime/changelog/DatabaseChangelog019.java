package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@ChangeUnit(id = "migrateBiologicalTypes", order = "019", author = "zaid", transactional = false)
public class DatabaseChangelog019 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        String collectionName = "biological_types";

        // Check if the collection already exists
        boolean collectionExists = mongoTemplate.collectionExists(collectionName);
        if (!collectionExists) {
            mongoTemplate.createCollection(collectionName);
        }

        // Check if records exist before inserting
        Query query = new Query();
        boolean recordsExist = mongoTemplate.exists(query, collectionName);
        if (!recordsExist) {
            List<Document> biologicalTypes = List.of(
                    // RADIOLOGY TYPES
                    new Document("name", "FIBROSIS").append("category", "RADIOLOGY"),
                    new Document("name", "CONSOLIDATION").append("category", "RADIOLOGY"),
                    new Document("name", "OPACITY").append("category", "RADIOLOGY"),
                    new Document("name", "NODULE").append("category", "RADIOLOGY"),
                    new Document("name", "CALCIFICATION").append("category", "RADIOLOGY"),

                    // PATHOLOGY TYPES
                    new Document("name", "PLEOMORPHISM").append("category", "PATHOLOGY"),
                    new Document("name", "NORMAL").append("category", "PATHOLOGY"),
                    new Document("name", "NERVES").append("category", "PATHOLOGY"),
                    new Document("name", "G1").append("category", "PATHOLOGY"),
                    new Document("name", "G2").append("category", "PATHOLOGY"),
                    new Document("name", "G3").append("category", "PATHOLOGY"),
                    new Document("name", "G4").append("category", "PATHOLOGY"),
                    new Document("name", "GX").append("category", "PATHOLOGY"),
                    new Document("name", "NUCLEOLI").append("category", "PATHOLOGY"),
                    new Document("name", "BLOOD_VESSELS").append("category", "PATHOLOGY"),
                    new Document("name", "MITOSIS_METAPHASE").append("category", "PATHOLOGY"),
                    new Document("name", "TUBULE_FORMATION").append("category", "PATHOLOGY"),
                    new Document("name", "NUCLEAR_MEMBRANE").append("category", "PATHOLOGY"),
                    new Document("name", "CRIBRIFORM_PATTERN").append("category", "PATHOLOGY"),
                    new Document("name", "INTRADUCTAL_CARCINOMA").append("category", "PATHOLOGY"),
                    new Document("name", "SEMINAL_VESICLE_INVASION").append("category", "PATHOLOGY"),
                    new Document("name", "LYMPHOVASCULAR_INVASION").append("category", "PATHOLOGY"),
                    new Document("name", "PERINEURAL_INVASION").append("category", "PATHOLOGY"),
                    new Document("name", "EXTRAPROSTATIC_EXTENSION").append("category", "PATHOLOGY"),
                    new Document("name", "ADENOCARCINOMA").append("category", "PATHOLOGY"),
                    new Document("name", "OTHERS").append("category", "PATHOLOGY")
            );

            // Insert into MongoDB collection
            mongoTemplate.getCollection(collectionName).insertMany(biologicalTypes);
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        mongoTemplate.dropCollection("biological_types");
    }
}

package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.List;

@ChangeUnit(id = "addPredefinedOrgans", order = "031", author = "zaid", transactional = false)
public class DatabaseChangelog031 {

    private static final List<String> PREDEFINED_ORGANS = Arrays.asList(
        "BREAST",
        "LUNGS",
        "LIVER",
        "KIDNEYS",
        "PROSTATE",
        "BONE MARROW",
        "SKIN",
        "THYROID",
        "STOMACH",
        "SMALL INTESTINE",
        "LARGE INTESTINE (COLON & RECTUM)",
        "BLADDER",
        "PANCREAS",
        "LYMPH NODES",
        "MUSCLES",
        "NERVES",
        "BRAIN",
        "ESOPHAGUS",
        "GALLBLADDER",
        "SALIVARY GLANDS",
        "ADRENAL GLANDS",
        "TESTICLES",
        "UTERUS (ENDOMETRIUM)",
        "CERVIX",
        "OVARIES",
        "PLACENTA",
        "BONES",
        "BLOOD VESSELS (ARTERIES, VEINS)",
        "HEART (MYOCARDIUM)",
        "PERITONEUM (ABDOMINAL LINING)"
    );

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        for (String organName : PREDEFINED_ORGANS) {
            // Check if organ already exists
            Query query = new Query(Criteria.where("name").is(organName));
            boolean exists = mongoTemplate.exists(query, "organs");
            
            if (!exists) {
                // Create new organ document
                Document organ = new Document();
                organ.put("name", organName);
                mongoTemplate.getCollection("organs").insertOne(organ);
            }
        }
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        // Remove all predefined organs
        Document query = new Document("name", new Document("$in", PREDEFINED_ORGANS));
        mongoTemplate.getCollection("organs").deleteMany(query);
    }
} 
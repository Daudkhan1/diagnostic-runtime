package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "updateMicroMeterPerPixel", order = "020", author = "zaid", transactional = false)
public class DatabaseChangelog020 {

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        String collectionName = "patient_slides";

        // Update all documents where microMeterPerPixel is missing or empty
        Query query = new Query(Criteria.where("micro_meter_per_pixel").exists(false)
                .orOperator(Criteria.where("micro_meter_per_pixel").is(""),
                        Criteria.where("micro_meter_per_pixel").is(null)));

        Update update = new Update().set("micro_meter_per_pixel", "0.25");

        mongoTemplate.updateMulti(query, update, collectionName);
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        String collectionName = "patient_slides";

        // Rollback: Set micro_meter_per_pixel to null where it was previously set to 0.25
        Query query = new Query(Criteria.where("micro_meter_per_pixel").is("0.25"));
        Update update = new Update().set("micro_meter_per_pixel", null);

        mongoTemplate.updateMulti(query, update, collectionName);
    }
}

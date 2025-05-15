package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "updatePatientSlideStatus", order = "021", author = "zaid", transactional = false)
public class DatabaseChangelog021 {

    private static final String COLLECTION_NAME = "patient_slides";
    private static final String STATUS_FIELD = "status";
    private static final String DEFAULT_STATUS = "NEW";

    @Execution
    public void executeMigration(MongoTemplate mongoTemplate) {
        Query query = new Query(Criteria.where(STATUS_FIELD).is(null));
        Update update = new Update().set(STATUS_FIELD, DEFAULT_STATUS);

        mongoTemplate.updateMulti(query, update, COLLECTION_NAME);
    }

    @RollbackExecution
    public void rollbackMigration(MongoTemplate mongoTemplate) {
        Query query = new Query(Criteria.where(STATUS_FIELD).is(DEFAULT_STATUS));
        Update update = new Update().set(STATUS_FIELD, null);

        mongoTemplate.updateMulti(query, update, COLLECTION_NAME);
    }
}


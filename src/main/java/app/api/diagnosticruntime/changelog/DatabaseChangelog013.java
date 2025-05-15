package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;

@ChangeUnit(id = "addCreationDateToPatientSlides", order = "010", author = "zaid", transactional = true)
public class DatabaseChangelog013 {

    @Execution
    public void addCreationDate(MongoTemplate mongoTemplate) {
        mongoTemplate.updateMulti(
                new Query(),
                new Update().set("creation_date", LocalDate.now()),
                "patient_slides"
        );
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.updateMulti(
                new Query(),
                new Update().unset("creation_date"),
                "patient_slides"
        );
    }
}

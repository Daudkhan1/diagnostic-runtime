package app.api.diagnosticruntime.changelog;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@ChangeUnit(id = "updateCaseAndPatientSlideSchema", order = "007", author = "zaid", transactional = true)
public class DatabaseChangelog007 {

    @Execution
    public void migrateSchema(MongoTemplate mongoTemplate) {

        // Remove "Name" field from "cases"
        mongoTemplate.updateMulti(
                new Query(),
                new Update().unset("Name"),
                "cases"
        );

        // Rename "Case" to "name", "Organ" to "organ", "Status" to "status", and "Date" to "date"
        mongoTemplate.updateMulti(
                new Query(),
                new Update()
                        .rename("Case", "name")
                        .rename("Organ", "organ")
                        .rename("Status", "status")
                        .rename("Date", "date"),
                "cases"
        );

        // Convert "date" field to LocalDate
        mongoTemplate.findAll(org.bson.Document.class, "cases").forEach(doc -> {
            String dateString = (String) doc.get("date");
            if (dateString != null) {
                LocalDate localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_DATE);
                doc.put("date", localDate);
                mongoTemplate.save(doc, "cases");
            }
        });

        // Remove "slides" field from "patient_slides"
        mongoTemplate.updateMulti(
                new Query(),
                new Update().unset("slides"),
                "patient_slides"
        );
    }

    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        // Rollback logic: Revert field names and re-add removed fields if necessary
        mongoTemplate.updateMulti(
                new Query(),
                new Update()
                        .rename("name", "Case")
                        .rename("organ", "Organ")
                        .rename("status", "Status")
                        .rename("date", "Date"),
                "cases"
        );

        // Re-add "slides" field with a default value
        mongoTemplate.updateMulti(
                new Query(),
                new Update().set("slides", 0),
                "patient_slides"
        );
    }
}

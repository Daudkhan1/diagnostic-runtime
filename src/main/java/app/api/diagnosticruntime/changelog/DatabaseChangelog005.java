package app.api.diagnosticruntime.changelog;

import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@ChangeUnit(id = "addFieldCaseType", order = "005", author = "zaid", transactional = true)
public class DatabaseChangelog005 {

    @Execution
    public void updateCaseType(MongoTemplate mongoTemplate) {
        mongoTemplate.updateMulti(
                new Query(Criteria.where("Organ").regex("X-ray", "i")),  // case-insensitive match for "X-ray"
                new Update().set("case_type", CaseType.RADIOLOGY),
                "cases"
        );

        mongoTemplate.updateMulti(
                new Query(Criteria.where("Organ").not().regex("X-ray", "i")),
                new Update().set("case_type", CaseType.PATHOLOGY),
                "cases"
        );
    }

    @RollbackExecution
    public void rollbackCaseType(MongoTemplate mongoTemplate) {
        // Remove caseType field in rollback
        mongoTemplate.updateMulti(
                new Query(Criteria.where("caseType").exists(true)),
                new Update().unset("caseType"),
                "cases"
        );
    }
}

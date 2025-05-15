package app.api.diagnosticruntime.patient.casemanagment.history.model;


import app.api.diagnosticruntime.patient.casemanagment.model.CaseStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Data
@Document(collection = "case_history")
@CompoundIndex(name = "case_history_index", def = "{'caseId': 1, 'createdAt': -1}")
public class History {
    @Id
    private String id;

    @Field("case_id")
    private String caseId;

    @Field("previous_status")
    private CaseStatus previousStatus;

    @Field("new_status")
    private CaseStatus newStatus;

    @Field("action_by")
    private String actionByPathologistId;

    @Field("transferred_to")
    private String transferredToPathologistId;

    @Field("created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Field("note")
    private String note;
}

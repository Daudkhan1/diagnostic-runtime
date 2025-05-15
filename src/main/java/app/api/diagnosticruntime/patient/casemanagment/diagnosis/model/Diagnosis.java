package app.api.diagnosticruntime.patient.casemanagment.diagnosis.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "diagnosis")
public class Diagnosis {
    @Id
    private String id;

    @Field("case_id")
    private String caseId;

    @Field("gross")
    private String gross;

    @Field("microscopy")
    private String microscopy;

    @Field("diagnosis")
    private String diagnosis;

    @Field("user_id")
    private String userId;

    @Field("is_deleted")
    private boolean isDeleted;
}


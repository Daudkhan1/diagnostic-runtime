package app.api.diagnosticruntime.patient.casemanagment.feedback.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@Document(collection = "feedback")
public class Feedback {
    @Id
    private String id;

    @Field("case_id")
    private String caseId;

    @Field("user_id")
    private String userId;

    @Field("difficulty_level")
    private int difficultyLevel;

    @Field("feedback")
    private String feedback;

    @Field("is_deleted")
    private boolean isDeleted;
}

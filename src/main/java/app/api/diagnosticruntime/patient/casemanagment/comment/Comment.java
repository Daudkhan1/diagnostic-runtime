package app.api.diagnosticruntime.patient.casemanagment.comment;

import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Data
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    @Field("case_id")
    private String caseId;

    @Field("creation_date")
    private LocalDate creationDate;

    @Field("modification_date")
    private LocalDate modificationDate;

    @Field("creation_user")
    private String creationUser;

    @Field("modification_user")
    private String modificationUser;

    @Field("comment_text")
    private String commentText;

    @Field("is_deleted")
    private boolean isDeleted;
}

package app.api.diagnosticruntime.patient.casemanagment.comment;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CommentDTO {
    private String id;

    private String caseId;

    private LocalDate creationDate;

    private String creationUserFullName;

    private String creationUser;

    private String commentText;

    private String modificationUser;

    private LocalDate modificationDate;

}

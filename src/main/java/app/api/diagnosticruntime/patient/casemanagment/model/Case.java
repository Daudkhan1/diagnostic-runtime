package app.api.diagnosticruntime.patient.casemanagment.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Data
@Document(collection = "cases")
@CompoundIndex(name = "referred_to_index", def = "{'referred_to': 1, 'is_deleted': 1}")
public class Case {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("status")
    private CaseStatus status;

    @Field("date")
    private LocalDateTime date;

    @Field("case_type")
    private CaseType caseType;

    @Field("patient_id")
    private String patientId;

    @Field("is_deleted")
    private boolean isDeleted;

    @Field("referred_to")
    private String referredTo;
}

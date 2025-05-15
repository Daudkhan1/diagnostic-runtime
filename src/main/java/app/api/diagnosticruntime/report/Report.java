package app.api.diagnosticruntime.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Document(collection = "reports")
public class Report {
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
}

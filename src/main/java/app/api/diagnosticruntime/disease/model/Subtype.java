package app.api.diagnosticruntime.disease.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Document(collection = "subtypes")
public class Subtype {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("organ_id")
    private String organId;
} 
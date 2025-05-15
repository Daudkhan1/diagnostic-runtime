package app.api.diagnosticruntime.organ.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Document(collection = "organs")
public class Organ {
    @Id
    private String id;

    @Field("name")
    @Indexed(unique = true)
    private String name;
} 
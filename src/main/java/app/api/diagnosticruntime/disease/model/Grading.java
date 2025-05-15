package app.api.diagnosticruntime.disease.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Document(collection = "gradings")
public class Grading {
    @Id
    private String id;

    @Field("name")
    private String name;

    @Field("disease_spectrum_id")
    private String diseaseSpectrumId;
} 
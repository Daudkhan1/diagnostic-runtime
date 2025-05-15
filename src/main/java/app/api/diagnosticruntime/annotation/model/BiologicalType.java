package app.api.diagnosticruntime.annotation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@Data
@Document(collection = "biological_types")
public class BiologicalType {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;
    private String category;
}
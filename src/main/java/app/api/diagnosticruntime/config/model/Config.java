package app.api.diagnosticruntime.config.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "config")
public class Config {

    @Id
    private String id;
    private String key;
    private String value;
}

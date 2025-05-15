package app.api.diagnosticruntime.userdetails.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Setter
@Getter
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    @Field("role")
    private String roleName;
}

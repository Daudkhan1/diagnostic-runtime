package app.api.diagnosticruntime.annotation.dto;

import app.api.diagnosticruntime.annotation.model.Coordinate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AIAnnotationCreationData {
    private String name;
    private String biological_type;
    private String shape;
    private Float confidence;
    private String description;
    private Set<Coordinate> coordinates;
}

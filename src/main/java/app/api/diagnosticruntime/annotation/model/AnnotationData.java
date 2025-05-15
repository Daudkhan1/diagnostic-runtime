package app.api.diagnosticruntime.annotation.model;

import lombok.Data;

import java.util.Set;

@Data
public class AnnotationData {

    private String name;
    private String type;
    private String shape;
    private String description;
    private Set<Coordinate> coordinates;
}



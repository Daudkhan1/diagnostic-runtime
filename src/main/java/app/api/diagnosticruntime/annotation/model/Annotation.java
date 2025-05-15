package app.api.diagnosticruntime.annotation.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Set;

@Data
@Document(collection = "annotations")
public class Annotation {
    @Id
    private String id;

    @Field("patient_slide_id")
    private String patientSlideId;

    @Field("annotation_type")
    private AnnotationType annotationType;

    @Field("name")
    private String name;

    @Field("biological_type")
    private String biologicalType;

    @Field("shape")
    private String shape;

    @Field("description")
    private String description;

    @Field("coordinates")
    private Set<Coordinate> coordinates;

    @Field("manual_coordinates")
    private Set<ManualCoordinate> manualCoordinates;

    @Field("annotator_coordinates")
    private String annotatorCoordinates;

    @Field("color")
    private String color;

    @Field("is_deleted")
    private boolean isDeleted;

    @Field("last_modified_user")
    private String lastModifiedUser;

    @Field("created_by")
    private String createdBy;

    @Field("state")
    private AnnotationState state;

    @Field("disease_spectrum_id")
    private String diseaseSpectrumId;

    @Field("subtype_id")
    private String subtypeId;

    @Field("grading_id")
    private String gradingId;

    @Field("confidence")
    private Float confidence;
}


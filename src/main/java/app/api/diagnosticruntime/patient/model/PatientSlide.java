package app.api.diagnosticruntime.patient.model;

import jakarta.persistence.Id;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Getter
@Setter
@Data
@Document(collection = "patient_slides")
public class PatientSlide {

    @Id
    private String id;

    @Field("annotation")
    private int annotation;

    @Field("case_id")
    private String caseId;

    @Field("slide_image_path")
    private String slideImagePath;

    @Field("show_image_path")
    private String showImagePath;

    @Field("creation_date")
    private LocalDate creationDate;

    @Field("is_deleted")
    private boolean isDeleted;

    @Field("micro_meter_per_pixel")
    private String microMeterPerPixel;

    @Field("status")
    private PatientSlideStatus status;

    @Field("organ_id")
    private String organId;
}

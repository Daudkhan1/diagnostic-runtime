package app.api.diagnosticruntime.patient.dto;

import app.api.diagnosticruntime.annotation.dto.AnnotationDTO;
import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PatientSlideGetDTO {

    private String id;

    private Long annotationCount;

    private String caseId;

    private String slideImagePath;

    private String showImagePath;

    private LocalDate creationDate;

    private List<AnnotationDTO> annotations;

    private String microMeterPerPixel;

    private PatientSlideStatus status;

    private String organ;
}

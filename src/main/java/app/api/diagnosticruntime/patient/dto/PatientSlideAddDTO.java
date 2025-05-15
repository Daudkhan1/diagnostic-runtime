package app.api.diagnosticruntime.patient.dto;

import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Field;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PatientSlideAddDTO {

    private String caseId;

    private String slideImagePath;

    private String showImagePath;

    private String microMeterPerPixel;

    private PatientSlideStatus status;

    private String organ;
}

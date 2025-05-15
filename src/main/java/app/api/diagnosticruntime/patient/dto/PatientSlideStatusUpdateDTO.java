package app.api.diagnosticruntime.patient.dto;

import app.api.diagnosticruntime.patient.model.PatientSlideStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PatientSlideStatusUpdateDTO {
    private PatientSlideStatus status;
}

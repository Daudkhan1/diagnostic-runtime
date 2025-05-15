package app.api.diagnosticruntime.patient.dto;

import lombok.Data;

@Data
public class PatientDetailsDTO {
    private String praidId;
    private String id;
    private String gender;
    private Integer age;
    private String mrn;
}

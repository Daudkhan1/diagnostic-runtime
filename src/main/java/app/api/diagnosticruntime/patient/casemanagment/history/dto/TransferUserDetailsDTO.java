package app.api.diagnosticruntime.patient.casemanagment.history.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TransferUserDetailsDTO {

    private String fullName;

    private String userRole;

    private LocalDateTime transferredDate;
}

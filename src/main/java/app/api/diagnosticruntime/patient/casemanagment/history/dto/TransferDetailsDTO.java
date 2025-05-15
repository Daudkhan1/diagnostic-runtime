package app.api.diagnosticruntime.patient.casemanagment.history.dto;

import lombok.Data;

@Data
public class TransferDetailsDTO {
    private TransferUserDetailsDTO transferredTo;
    private TransferUserDetailsDTO transferredBy;
}

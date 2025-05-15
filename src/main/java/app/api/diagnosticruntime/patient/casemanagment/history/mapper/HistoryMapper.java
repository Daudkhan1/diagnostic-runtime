package app.api.diagnosticruntime.patient.casemanagment.history.mapper;

import app.api.diagnosticruntime.patient.casemanagment.history.dto.TransferUserDetailsDTO;
import app.api.diagnosticruntime.patient.casemanagment.history.model.History;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;

public class HistoryMapper {

    public static TransferUserDetailsDTO fromHistory(History history, UserInfoDTO user) {
        TransferUserDetailsDTO transferUserDetailsDTO = new TransferUserDetailsDTO();
        transferUserDetailsDTO.setTransferredDate(history.getCreatedAt());
        transferUserDetailsDTO.setUserRole(user.getRole());
        transferUserDetailsDTO.setFullName(user.getFullName());
        return transferUserDetailsDTO;
    }
}

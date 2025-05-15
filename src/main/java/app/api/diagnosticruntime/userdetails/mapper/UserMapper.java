package app.api.diagnosticruntime.userdetails.mapper;

import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.model.User;

public class UserMapper {

    public static UserInfoDTO toUserInfoDTO(User user) {
        UserInfoDTO dto = new UserInfoDTO();
        dto.setId(user.getId());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPassword());
        dto.setStatus(user.getStatus().toString());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRegistrationDate(user.getRegistrationDate());
        dto.setRole(user.getRole().toString());
        return dto;
    }
}

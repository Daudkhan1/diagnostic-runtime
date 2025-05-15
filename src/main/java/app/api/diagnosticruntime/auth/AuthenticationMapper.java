package app.api.diagnosticruntime.auth;

import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;

public class AuthenticationMapper {

    public static AuthenticationResponse toAuthenticationMapper(String token, UserInfoDTO infoDetails) {
        AuthenticationResponse authenticationResponse = new AuthenticationResponse();
        authenticationResponse.setToken(token);
        authenticationResponse.setId(infoDetails.getId());
        authenticationResponse.setFullName(infoDetails.getFullName());
        authenticationResponse.setEmail(infoDetails.getEmail());
        authenticationResponse.setStatus(infoDetails.getStatus());
        authenticationResponse.setPhoneNumber(infoDetails.getPhoneNumber());
        authenticationResponse.setRole(infoDetails.getRole());
        authenticationResponse.setRegistrationDate(infoDetails.getRegistrationDate());
        return authenticationResponse;
    }
}

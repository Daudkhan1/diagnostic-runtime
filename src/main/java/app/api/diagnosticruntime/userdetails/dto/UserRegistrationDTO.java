package app.api.diagnosticruntime.userdetails.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Setter
@Getter
public class UserRegistrationDTO {
    private String fullName;
    private String email;
    private String password;
    private String role;
    private String phoneNumber;
}

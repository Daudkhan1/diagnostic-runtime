package app.api.diagnosticruntime.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class AuthenticationResponse {
    private String id;
    private String fullName;
    private String email;
    private String status;
    private String role;
    private String phoneNumber;
    private LocalDate registrationDate;
    public String token;
}

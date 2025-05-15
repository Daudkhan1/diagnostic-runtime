package app.api.diagnosticruntime.userdetails.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UserInfoDTO {
    private String id;
    private String fullName;
    private String email;
    private String password;
    private String status;
    private String phoneNumber;
    private LocalDate registrationDate;
    private String role;
}

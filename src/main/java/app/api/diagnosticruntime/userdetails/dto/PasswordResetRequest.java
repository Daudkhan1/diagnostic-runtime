package app.api.diagnosticruntime.userdetails.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PasswordResetRequest {
    private String email;
    private String newPassword;
}

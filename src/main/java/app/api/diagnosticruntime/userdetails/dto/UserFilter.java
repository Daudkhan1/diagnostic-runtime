package app.api.diagnosticruntime.userdetails.dto;

import app.api.diagnosticruntime.userdetails.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UserFilter {
    private UserStatus status;
    private String role;
    private String fullname;
}

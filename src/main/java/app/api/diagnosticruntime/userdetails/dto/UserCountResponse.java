package app.api.diagnosticruntime.userdetails.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class UserCountResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long rejectedUsers;
    private long pendingUsers;
}

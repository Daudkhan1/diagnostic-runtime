package app.api.diagnosticruntime.dashboard;

import app.api.diagnosticruntime.patient.casemanagment.dto.CaseCountResponse;
import app.api.diagnosticruntime.userdetails.dto.UserCountResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class DashboardOverviewDTO {
    private UserCountResponse users;
    private CaseCountResponse cases;
    private Long patients;
}

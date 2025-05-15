package app.api.diagnosticruntime.dashboard;

import app.api.diagnosticruntime.patient.casemanagment.dto.CaseCountResponse;
import app.api.diagnosticruntime.patient.casemanagment.service.CaseService;
import app.api.diagnosticruntime.patient.service.PatientService;
import app.api.diagnosticruntime.userdetails.dto.UserCountResponse;
import app.api.diagnosticruntime.userdetails.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;

    private final CaseService caseService;

    private final PatientService patientService;

    @GetMapping
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview() {
        UserCountResponse users = userService.getUserCounts();
        CaseCountResponse cases = caseService.getCaseCount();
        Long patients = patientService.getTotalPatients();
        DashboardOverviewDTO dashboard = new DashboardOverviewDTO(users, cases, patients);
        return new ResponseEntity<>(dashboard, HttpStatus.OK);
    }
}

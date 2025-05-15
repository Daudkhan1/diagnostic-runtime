package app.api.diagnosticruntime.annotation.service;

import app.api.diagnosticruntime.patient.casemanagment.history.service.HistoryService;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnnotationPermissionService {

    private final HistoryService historyService;
    private final PatientSlideService patientSlideService;

    public boolean hasPermissionToAnnotate(String patientSlideId, String username) {
        PatientSlide patientSlide = patientSlideService.getPatientSlideById(patientSlideId);
        return historyService.hasPermissionToAnnotate(patientSlide.getCaseId(), username);
    }

}

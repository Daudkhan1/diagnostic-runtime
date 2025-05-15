package app.api.diagnosticruntime.patient.controller;

import app.api.diagnosticruntime.annotation.ai.service.TaskProcessorService;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.patient.dto.*;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.mapper.PatientSlideMapper;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.casemanagment.service.CaseService;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/patient/slide")
@RequiredArgsConstructor
public class PatientSlideController {

    private final PatientSlideService patientSlideService;
    private final TaskProcessorService taskProcessorService;
    private final AnnotationService annotationService;
    private final CaseService caseService;
    private final PatientSlideMapper patientSlideMapper;


    @GetMapping("/{id}")
    public ResponseEntity<PatientSlideGetDTO> getPatientSlide(@PathVariable String id) {
        PatientSlide patientSlide = patientSlideService.getPatientSlideById(id);
        List<Annotation> annotations =  annotationService.getAllAnnotationsForPatientSlide(id);
        return ResponseEntity.ok(patientSlideMapper.toPatientSlideGetDTO(patientSlide, annotations));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<PatientSlideDetailsDTO> getSlideDetails(@PathVariable String id) {
        PatientSlideDetailsDTO slideDetails = patientSlideService.getSlideDetails(id);
        return ResponseEntity.ok(slideDetails);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePatientSlideById(@PathVariable String id, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            patientSlideService.deleteById(id, userDetails.getUsername());
            return ResponseEntity.status(HttpStatus.OK).body("Deleted successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Patient slide of id "+id+" not found");
        } catch (Exception e) {
            log.error("Exception while deleting Patient slide of id "+id+" "+ e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PostMapping
    public ResponseEntity<PatientSlideDTO> addSlide(@RequestBody PatientSlideAddDTO slide) {
        PatientSlide uploadedSlide = patientSlideService.createPatientSlide(slide);
        Case patientCase = caseService.getCase(uploadedSlide.getCaseId());
        taskProcessorService.enqueueTask(uploadedSlide.getId(), uploadedSlide.getSlideImagePath(), patientCase.getCaseType());
        return ResponseEntity.status(HttpStatus.CREATED).body(patientSlideMapper.toPatientSlideDTO(uploadedSlide));
    }

    @GetMapping
    public ResponseEntity<Page<PatientSlideDTO>> getAllPatientSlides(
            @RequestParam(required = false) String caseId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            Pageable pageable) {

        Page<PatientSlideDTO> patientSlides = patientSlideService.getAllPatientSlides(caseId, fromDate, toDate, pageable);
        return ResponseEntity.ok(patientSlides);
    }

    @PostMapping("/annotation/ai")
    public ResponseEntity<String> createAIAnnotations(@RequestParam String slideId) {
        try {
            PatientSlide patientSlide = patientSlideService.getPatientSlideById(slideId);
            Case patientSlideCase = caseService.getCase(patientSlide.getCaseId());
            taskProcessorService.enqueueTask(slideId, patientSlide.getSlideImagePath(), patientSlideCase.getCaseType());
            return ResponseEntity.ok("AI annotations created successfully for slide ID: " + slideId);
        } catch (Exception e) {
            // Log the exception and return an appropriate response
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create AI annotations: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> updatePatientSlideStatus(@PathVariable String id, @RequestBody PatientSlideStatusUpdateDTO status) {
        patientSlideService.updatePatientStatus(id, status.getStatus());
        return ResponseEntity.ok("Patient Slide Updated Successfully");
    }

    @PutMapping("/case/{caseId}status")
    public ResponseEntity<String> updatePatientSlideStatus(@PathVariable String id, @PathVariable String caseId, @RequestBody PatientSlideStatusUpdateDTO status) {
        patientSlideService.updateStatusOfAllCaseSlides(caseId, status.getStatus());
        return ResponseEntity.ok("Patient Slide Updated Successfully");
    }
}

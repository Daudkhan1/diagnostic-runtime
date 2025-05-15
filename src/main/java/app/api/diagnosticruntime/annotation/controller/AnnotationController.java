package app.api.diagnosticruntime.annotation.controller;

import app.api.diagnosticruntime.annotation.dto.AIAnnotatonCreationDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationDetailsDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationUpdateDTO;
import app.api.diagnosticruntime.annotation.mapper.AnnotationMapper;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.annotation.model.AnnotationState;
import app.api.diagnosticruntime.annotation.model.AnnotationType;
import app.api.diagnosticruntime.annotation.model.BiologicalType;
import app.api.diagnosticruntime.annotation.service.AnnotationService;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.patient.service.PatientSlideService;
import app.api.diagnosticruntime.userdetails.model.User;
import app.api.diagnosticruntime.userdetails.model.UserRole;
import app.api.diagnosticruntime.userdetails.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AnnotationController {
    private final AnnotationService annotationService;
    private final UserService userService;
    private final AnnotationMapper annotationMapper;
    private final PatientSlideService patientSlideService;

    @PostMapping("/slide/annotation")
    @PreAuthorize("@annotationPermissionService.hasPermissionToAnnotate(#annotationDTO.patientSlideId, #userDetails.username)")
    public ResponseEntity<AnnotationDTO> createAnnotation(@RequestBody AnnotationDTO annotationDTO, @AuthenticationPrincipal UserDetails userDetails) {
        AnnotationDTO annotationWithParams = annotationService.createAnnotationParams(annotationDTO);
        Annotation createdAnnotation =  annotationService.createAnnotation(annotationMapper.toAnnotation(annotationWithParams), annotationWithParams.getCaseType(), userDetails.getUsername());
        return ResponseEntity.ok(annotationMapper.toAnnotationDTO(createdAnnotation));
    }

    @PutMapping("/slide/{slideId}/annotations/{annotationId}")
    public ResponseEntity<Annotation> updateAnnotation(
            @PathVariable String slideId,
            @PathVariable String annotationId,
            @RequestBody AnnotationUpdateDTO updateDTO,
            @RequestParam CaseType caseType,
            @AuthenticationPrincipal UserDetails userDetails) {

        Annotation updatedAnnotation = annotationService.updateAnnotation(
                slideId,
                annotationId,
                updateDTO,
                caseType,
                userDetails.getUsername()
        );

        return ResponseEntity.ok(updatedAnnotation);
    }

    @PostMapping("/slide/annotation/ai")
    public ResponseEntity<Void> createAiAnnotation(@RequestBody AIAnnotatonCreationDTO aiAnnotatonCreationDTO, @RequestParam String patientSlideId, @RequestParam boolean radiology) {
        PatientSlide patientSlide = patientSlideService.getPatientSlideById(patientSlideId);
        annotationService.createAIAnnotations(aiAnnotatonCreationDTO, patientSlide, radiology);
        return ResponseEntity.ok().build();
    }

    // Delete annotations by slideId and annotationId
    @DeleteMapping("/slide/{slideId}/annotation/{annotationId}")
    public ResponseEntity<Void> deleteSlideAnnotationsById(@PathVariable String slideId, @PathVariable String annotationId,
                                                           @AuthenticationPrincipal UserDetails userDetails) {
        annotationService.deleteAnnotationById(slideId, annotationId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Get annotation by slideId and annotationId
    @GetMapping("/slide/{slideId}/annotation/{annotationId}")
    public ResponseEntity<AnnotationDTO> getAnnotationById(@PathVariable String slideId, @PathVariable String annotationId) {
        return annotationService.findByIdAndSlideId(annotationId, slideId)
                .map(annotationMapper::toAnnotationDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/slide/{slideId}/annotation/{annotationId}/details")
    public ResponseEntity<AnnotationDetailsDTO> getAnnotationDetailsById(@PathVariable String slideId, @PathVariable String annotationId) {
        Annotation annotation =  annotationService.findById(annotationId);
        String lastModifiedUser = annotation.getLastModifiedUser();
        User user = new User();
        if(lastModifiedUser.contains("SYSTEM") || lastModifiedUser.equals("MODEL")){
            user.setRole(UserRole.AI);
            user.setFullName("AI MODEL");
        }
        else {
            user = userService.getUserByUsername(lastModifiedUser);
        }

        return ResponseEntity.ok(annotationMapper.toAnnotationDetailsDTO(annotation, user));
    }

    @GetMapping("/slide/annotation")
    public ResponseEntity<List<AnnotationDTO>> getAllAnnotations() {
        List<AnnotationDTO> annotations = annotationService.findAll().stream()
                .map(annotationMapper::toAnnotationDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(annotations);
    }


    // Delete all annotations for a slide
    @DeleteMapping("/slide/{slideId}/annotation")
    public ResponseEntity<Void> deleteAllAnnotationsForPatientSlides(@PathVariable String slideId, @AuthenticationPrincipal UserDetails userDetails) {
        annotationService.deleteAllAnnotationsForSlide(slideId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    // Get all annotations for a slide
    @GetMapping("/slide/{slideId}/annotation")
    public ResponseEntity<List<AnnotationDTO>> getAllAnnotationsForPatientSlide(@PathVariable String slideId) {
        List<AnnotationDTO> annotations = annotationService.getAllAnnotationsForPatientSlide(slideId).stream()
                .map(annotationMapper::toAnnotationDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(annotations);
    }

    @GetMapping("/slide/annotation/type")
    public ResponseEntity<List<AnnotationType>> getAllAnnotationsTypes() {
        List<AnnotationType> statuses = Arrays.asList(AnnotationType.values());
        return new ResponseEntity<>(statuses, HttpStatus.OK);
    }

    @GetMapping("/slide/annotation/type/biological/{type}")
    public ResponseEntity<List<String>> getAllAnnotationsBiologicalTypes(@PathVariable CaseType type) {
        List<BiologicalType> statuses = annotationService.getBiologicalTypeByCategory(type);
        return new ResponseEntity<>(statuses.stream().map(BiologicalType::getName).collect(Collectors.toList()), HttpStatus.OK);
    }

    @PutMapping("/slide/{slideId}/annotations/{annotationId}/state")
    public ResponseEntity<Annotation> updateAnnotationState(
            @PathVariable String slideId,
            @PathVariable String annotationId,
            @RequestParam AnnotationState state,
            @AuthenticationPrincipal UserDetails userDetails) {

        Annotation updatedAnnotation = annotationService.updateAnnotationState(
            slideId,
            annotationId,
            state,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(updatedAnnotation);
    }

    @PutMapping("/slide/{slideId}/annotations/state")
    public ResponseEntity<List<Annotation>> updateAnnotationStatesInBulk(
            @PathVariable String slideId,
            @RequestParam AnnotationState state,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<Annotation> updatedAnnotations = annotationService.updateAnnotationStatesInBulk(
            slideId,
            state,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(updatedAnnotations);
    }
}


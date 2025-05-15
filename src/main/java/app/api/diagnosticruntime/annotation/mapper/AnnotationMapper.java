package app.api.diagnosticruntime.annotation.mapper;

import app.api.diagnosticruntime.annotation.dto.AnnotationDTO;
import app.api.diagnosticruntime.annotation.dto.AnnotationDetailsDTO;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.disease.dto.DiseaseSpectrumDTO;
import app.api.diagnosticruntime.disease.dto.GradingDTO;
import app.api.diagnosticruntime.disease.dto.SubtypeDTO;
import app.api.diagnosticruntime.disease.service.DiseaseSpectrumService;
import app.api.diagnosticruntime.disease.service.GradingService;
import app.api.diagnosticruntime.disease.service.SubtypeService;
import app.api.diagnosticruntime.userdetails.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AnnotationMapper {
    private final DiseaseSpectrumService diseaseSpectrumService;
    private final SubtypeService subtypeService;
    private final GradingService gradingService;


    public AnnotationDTO toAnnotationDTO(Annotation annotation) {
        AnnotationDTO dto = new AnnotationDTO();
        dto.setId(annotation.getId());
        dto.setPatientSlideId(annotation.getPatientSlideId());
        dto.setAnnotationType(annotation.getAnnotationType());
        dto.setName(annotation.getName());
        dto.setBiologicalType(annotation.getBiologicalType());
        dto.setShape(annotation.getShape());
        dto.setDescription(annotation.getDescription());
        dto.setCoordinates(annotation.getCoordinates());
        dto.setManualCoordinates(annotation.getManualCoordinates());
        dto.setAnnotatorCoordinates(annotation.getAnnotatorCoordinates());
        dto.setColor(annotation.getColor());
        dto.setIsDeleted(annotation.isDeleted());
        dto.setLastModifiedUser(annotation.getLastModifiedUser());
        dto.setState(annotation.getState());

        // Map the new fields
        if (annotation.getDiseaseSpectrumId() != null) {
            dto.setDiseaseSpectrum(diseaseSpectrumService.getDiseaseSpectrumById(annotation.getDiseaseSpectrumId()));
        }
        if (annotation.getSubtypeId() != null) {
            dto.setSubtype(subtypeService.getSubtypeById(annotation.getSubtypeId()));
        }
        if (annotation.getGradingId() != null) {
            dto.setGrading(gradingService.getGradingById(annotation.getGradingId()));
        }

        return dto;
    }

    public List<AnnotationDTO> toAnnotationDtoList(List<Annotation> annotations) {
        return annotations.stream().map(this::toAnnotationDTO).toList();
    }

    public Annotation toAnnotation(AnnotationDTO dto) {
        Annotation annotation = new Annotation();
        annotation.setId(dto.getId());
        annotation.setPatientSlideId(dto.getPatientSlideId());
        annotation.setAnnotationType(dto.getAnnotationType());
        annotation.setName(dto.getName());
        annotation.setBiologicalType(dto.getBiologicalType());
        annotation.setShape(dto.getShape());
        annotation.setDescription(dto.getDescription());
        annotation.setCoordinates(dto.getCoordinates());
        annotation.setManualCoordinates(dto.getManualCoordinates());
        annotation.setAnnotatorCoordinates(dto.getAnnotatorCoordinates());
        annotation.setColor(dto.getColor());
        annotation.setDeleted(dto.getIsDeleted());
        annotation.setLastModifiedUser(dto.getLastModifiedUser());
        annotation.setState(dto.getState());

        // Map the new fields
        if (dto.getDiseaseSpectrum() != null) {
            annotation.setDiseaseSpectrumId(dto.getDiseaseSpectrum().getId());
        }
        if (dto.getSubtype() != null) {
            annotation.setSubtypeId(dto.getSubtype().getId());
        }
        if (dto.getGrading() != null) {
            annotation.setGradingId(dto.getGrading().getId());
        }

        return annotation;
    }

    public  AnnotationDetailsDTO toAnnotationDetailsDTO(Annotation annotation, User user) {
        AnnotationDetailsDTO annotationDetailsDTO = new AnnotationDetailsDTO();
        annotationDetailsDTO.setAnnotatedBy(user.getFullName());
        annotationDetailsDTO.setShape(annotation.getShape());
        annotationDetailsDTO.setRole(user.getRole().toString());
        annotationDetailsDTO.setConfidence(annotationDetailsDTO.getConfidence());
        annotationDetailsDTO.setStatus(annotation.getState());
        annotationDetailsDTO.setName(annotation.getName());
        annotationDetailsDTO.setId(annotation.getId());
        annotationDetailsDTO.setDescription(annotation.getDescription());
        annotationDetailsDTO.setBiologicalType(annotation.getBiologicalType());
        annotationDetailsDTO.setAnnotationType(annotation.getAnnotationType().toString());
        // Map the new fields
        if (annotation.getDiseaseSpectrumId() != null) {
            DiseaseSpectrumDTO diseaseSpectrum = diseaseSpectrumService.getDiseaseSpectrumById(annotation.getDiseaseSpectrumId());
            annotationDetailsDTO.setDiseaseSpectrum(diseaseSpectrum.getName());
        }
        if (annotation.getSubtypeId() != null) {
            SubtypeDTO subtype = subtypeService.getSubtypeById(annotation.getSubtypeId());
            annotationDetailsDTO.setSubType(subtype.getName());
        }
        if (annotation.getGradingId() != null) {
            GradingDTO gradingDTO = gradingService.getGradingById(annotation.getGradingId());
            annotationDetailsDTO.setGrading(gradingDTO.getName());
        }
        return annotationDetailsDTO;
    }
}

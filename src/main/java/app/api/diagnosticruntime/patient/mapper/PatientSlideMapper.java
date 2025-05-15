package app.api.diagnosticruntime.patient.mapper;

import app.api.diagnosticruntime.annotation.dto.AnnotatorDetailsDTO;
import app.api.diagnosticruntime.annotation.mapper.AnnotationMapper;
import app.api.diagnosticruntime.annotation.model.Annotation;
import app.api.diagnosticruntime.patient.casemanagment.model.Case;
import app.api.diagnosticruntime.patient.dto.PatientSlideAddDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideDetailsDTO;
import app.api.diagnosticruntime.patient.dto.PatientSlideGetDTO;
import app.api.diagnosticruntime.patient.model.PatientSlide;
import app.api.diagnosticruntime.organ.service.OrganService;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@RequiredArgsConstructor
public class PatientSlideMapper {
    private final OrganService organService;
    private final AnnotationMapper annotationMapper;

    public PatientSlide toPatientSlide(PatientSlideAddDTO dto) {
        PatientSlide patientSlide = new PatientSlide();
        patientSlide.setCaseId(dto.getCaseId());
        patientSlide.setSlideImagePath(dto.getSlideImagePath());
        patientSlide.setShowImagePath(dto.getShowImagePath());
        patientSlide.setMicroMeterPerPixel(dto.getMicroMeterPerPixel());
        patientSlide.setStatus(dto.getStatus());
        if(!Strings.isEmpty(dto.getOrgan()))
            patientSlide.setOrganId(organService.getOrCreateOrganId(dto.getOrgan()));
       else
            patientSlide.setOrganId(dto.getOrgan());
        return patientSlide;
    }

    public PatientSlideDTO toPatientSlideDTO(PatientSlide patientSlide) {
        PatientSlideDTO dto = new PatientSlideDTO();
        dto.setId(patientSlide.getId());
        dto.setCaseId(patientSlide.getCaseId());
        dto.setSlideImagePath(patientSlide.getSlideImagePath());
        dto.setShowImagePath(patientSlide.getShowImagePath());
        dto.setCreationDate(patientSlide.getCreationDate());
        dto.setMicroMeterPerPixel(patientSlide.getMicroMeterPerPixel());
        dto.setStatus(patientSlide.getStatus());
        if(!Strings.isEmpty(patientSlide.getOrganId()))
            dto.setOrgan(organService.getOrganNameFromId(patientSlide.getOrganId()));
        else
            dto.setOrgan(patientSlide.getOrganId());
        return dto;
    }

    public PatientSlideGetDTO toPatientSlideGetDTO(PatientSlide patientSlide, List<Annotation> annotationList) {
        PatientSlideGetDTO patientSlideToMap = new PatientSlideGetDTO();
        patientSlideToMap.setId(patientSlide.getId());
        patientSlideToMap.setAnnotationCount((long) annotationList.size());
        patientSlideToMap.setCaseId(patientSlide.getCaseId());
        patientSlideToMap.setSlideImagePath(patientSlide.getSlideImagePath());
        patientSlideToMap.setShowImagePath(patientSlide.getShowImagePath());
        patientSlideToMap.setCreationDate(patientSlide.getCreationDate());
        patientSlideToMap.setAnnotations(annotationMapper.toAnnotationDtoList(annotationList));
        patientSlideToMap.setMicroMeterPerPixel(patientSlide.getMicroMeterPerPixel());
        patientSlideToMap.setStatus(patientSlide.getStatus());
        if(!Strings.isEmpty(patientSlide.getOrganId()))
            patientSlideToMap.setOrgan(organService.getOrganNameFromId(patientSlide.getOrganId()));
        else
            patientSlideToMap.setOrgan(patientSlide.getOrganId());
        return patientSlideToMap;
    }

    public PatientSlideDTO patientSlideDtoFromPatientSlide(PatientSlide slide, Long annotationCount) {
        return new PatientSlideDTO(
                slide.getId(),
                annotationCount,
                slide.getCaseId(),
                slide.getSlideImagePath(),
                slide.getShowImagePath(),
                slide.getCreationDate(),
                slide.getMicroMeterPerPixel(),
                slide.getStatus(),
                Strings.isEmpty(slide.getOrganId()) ?  slide.getOrganId() :
                organService.getOrganNameFromId(slide.getOrganId())
        );
    }

    public PatientSlideDetailsDTO patientSlideDetailsDTOFromPatientSlide(PatientSlide slide, long manualAnnotationCount,
                                                                         long aiAnnotationCount, Case caseDetails,
                                                                         List<AnnotatorDetailsDTO> annotationDetails
                                                                         ){
        PatientSlideDetailsDTO dto = new PatientSlideDetailsDTO();
        dto.setStatus(slide.getStatus().toString());
        dto.setAnnotations(manualAnnotationCount);
        dto.setAiAnnotations(aiAnnotationCount);
        dto.setCaseId(caseDetails.getId());
        if(!Strings.isEmpty(slide.getOrganId()))
            dto.setOrgan(organService.getOrganNameFromId(slide.getOrganId()));
        else
            dto.setOrgan(slide.getOrganId());
        dto.setAnnotationsDetails(annotationDetails);
        return dto;
    }
}

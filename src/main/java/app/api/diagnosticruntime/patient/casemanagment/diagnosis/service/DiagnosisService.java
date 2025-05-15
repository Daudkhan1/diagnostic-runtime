package app.api.diagnosticruntime.patient.casemanagment.diagnosis.service;


import app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto.DiagnosisDTO;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.dto.DiagnosisUpdateDTO;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.mapper.DiagnosisMapper;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.model.Diagnosis;
import app.api.diagnosticruntime.patient.casemanagment.diagnosis.repository.DiagnosisRepository;
import app.api.diagnosticruntime.patient.casemanagment.model.CaseType;
import app.api.diagnosticruntime.patient.model.Patient;
import app.api.diagnosticruntime.slides.service.S3Service;
import app.api.diagnosticruntime.userdetails.dto.UserInfoDTO;
import app.api.diagnosticruntime.userdetails.service.UserService;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DiagnosisService {

    private final DiagnosisRepository diagnosisRepository;
    private final UserService userService;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
    public boolean hasDiagnosis(String userId, String caseId) {
        return diagnosisRepository.findByUserIdAndCaseIdAndIsDeleted(userId, caseId, false).size() > 0;
    }

    public String createReport(Patient patient, String caseId, CaseType caseType) {

        List<Diagnosis> diagnoses = diagnosisRepository.findByCaseIdAndIsDeleted(caseId, false);
        if(diagnoses.size() < 1)
            throw new IllegalArgumentException("Report not found");

        // Create a byte array output stream to hold the PDF data
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Initialize PDF writer and document
        PdfWriter pdfWriter = new PdfWriter(byteArrayOutputStream);
        com.itextpdf.kernel.pdf.PdfDocument pdfDocument = new com.itextpdf.kernel.pdf.PdfDocument(pdfWriter);
        Document document = new Document(pdfDocument);

        Paragraph bigHeading = new Paragraph("PRAID")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(bigHeading);

        // Add the 4 small headings with their respective values
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("Gender: " + patient.getGender()).setFontSize(12).setBold());
        document.add(new Paragraph("Age: " + patient.getAge()).setFontSize(12).setBold());

        for(Diagnosis diagnosis: diagnoses) {
            // Add the paragraphs with headings
            Optional<UserInfoDTO> userInfo = userService.getUserById(diagnosis.getUserId());
            if(userInfo.isPresent()) {
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("Diagnosis").setFontSize(14).setBold().setUnderline());
                document.add(new Paragraph(diagnosis.getDiagnosis()).setFontSize(12));

                document.add(new Paragraph(diagnosis.getGross()).setFontSize(12));
                if(caseType.equals(CaseType.PATHOLOGY)) {
                    document.add(new Paragraph("\n"));
                    document.add(new Paragraph("Microscopy").setFontSize(14).setBold().setUnderline());
                    document.add(new Paragraph(diagnosis.getMicroscopy()).setFontSize(12));

                    document.add(new Paragraph("\n"));
                    document.add(new Paragraph("Report By User " + userInfo.get().getFullName()).setFontSize(14).setBold().setUnderline());
                    document.add(new Paragraph("Gross").setFontSize(14).setBold().setUnderline());
                }
            }
        }

        // Close the document
        document.close();

        return s3Service.getS3UrlFromFile(byteArrayOutputStream);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisDTO> getAllDiagnoses() {
        List<Diagnosis> diagnoses = diagnosisRepository.findByIsDeleted(false);
        return diagnoses.stream()
                .map(DiagnosisMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DiagnosisDTO getDiagnosisById(String id) {
        Diagnosis diagnosis = diagnosisRepository.findById(id).orElseThrow(() -> new RuntimeException("Diagnosis not found"));
        return DiagnosisMapper.toDTO(diagnosis);
    }

    @Transactional
    public DiagnosisDTO createDiagnosis(DiagnosisDTO diagnosisDTO) {
        Diagnosis diagnosis = DiagnosisMapper.toEntity(diagnosisDTO);
        diagnosis.setDeleted(false);
        Diagnosis savedDiagnosis = diagnosisRepository.save(diagnosis);
        return DiagnosisMapper.toDTO(savedDiagnosis);
    }

    @Transactional(readOnly = true)
    public List<DiagnosisDTO> getDiagnosesByUserIdAndCaseId(String userId, String caseId) {
        List<Diagnosis> diagnoses = diagnosisRepository.findByUserIdAndCaseIdAndIsDeleted(userId, caseId, false);
        return diagnoses.stream()
                .map(DiagnosisMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DiagnosisDTO> getDiagnosesByCaseId(String caseId) {
        List<Diagnosis> diagnoses = diagnosisRepository.findByCaseIdAndIsDeleted(caseId, false);
        return diagnoses.stream()
                .map(DiagnosisMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public DiagnosisDTO updateDiagnosis(String id, DiagnosisUpdateDTO diagnosisUpdateDTO) {
        Diagnosis existingDiagnosis = diagnosisRepository.findById(id).orElseThrow(() -> new RuntimeException("Diagnosis not found"));
        existingDiagnosis.setCaseId(diagnosisUpdateDTO.getCaseId());
        existingDiagnosis.setGross(diagnosisUpdateDTO.getGross());
        existingDiagnosis.setMicroscopy(diagnosisUpdateDTO.getMicroscopy());
        existingDiagnosis.setDiagnosis(diagnosisUpdateDTO.getDiagnosis());
        Diagnosis updatedDiagnosis = diagnosisRepository.save(existingDiagnosis);
        return DiagnosisMapper.toDTO(updatedDiagnosis);
    }

    @Transactional
    public void deleteDiagnosis(String id) {
        Diagnosis diagnosis = diagnosisRepository.findById(id).orElseThrow(() -> new RuntimeException("Diagnosis not found"));
        diagnosis.setDeleted(true);
        diagnosisRepository.save(diagnosis);
    }
}

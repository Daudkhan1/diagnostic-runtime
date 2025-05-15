package app.api.diagnosticruntime.report;

import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public Report saveReport(Report report) {
        return reportRepository.save(report);
    }

    public void deleteAllByCaseId(String caseId) {
        reportRepository.deleteAllByCaseId(caseId);
    }

    public Optional<Report> getReportByCaseId(String caseId) {
        return reportRepository.findByCaseId(caseId);
    }
}

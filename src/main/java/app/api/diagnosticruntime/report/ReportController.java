package app.api.diagnosticruntime.report;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Report> createReport(@RequestBody Report report) {
        reportService.deleteAllByCaseId(report.getCaseId());
        Report savedReport = reportService.saveReport(report);
        return ResponseEntity.ok(savedReport);
    }

    @GetMapping("/case/{caseId}")
    public ResponseEntity<Report> getReportByCaseId(@PathVariable String caseId) {
        Optional<Report> report = reportService.getReportByCaseId(caseId);
        return report.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}

package com.sitemaster.report;

import com.sitemaster.report.dto.ReportInfo;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Reports: list, preview as JSON, export as PDF / Excel / CSV. */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT:VIEW')")
    public List<ReportInfo> available() {
        return reportService.available();
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasAuthority('REPORT:VIEW')")
    public ReportTable preview(@PathVariable ReportType type,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @RequestParam(required = false) Long projectId) {
        return reportService.preview(type, new ReportCriteria(from, to, projectId));
    }

    @GetMapping("/{type}/export")
    @PreAuthorize("hasAuthority('REPORT:VIEW')")
    public ResponseEntity<byte[]> export(@PathVariable ReportType type,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) Long projectId,
                                         @RequestParam ExportFormat format) {
        ReportService.ExportedReport file = reportService.export(type, new ReportCriteria(from, to, projectId), format);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.fileName()).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(file.content());
    }
}

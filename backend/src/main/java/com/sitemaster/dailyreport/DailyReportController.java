package com.sitemaster.dailyreport;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.dailyreport.dto.DailyReportRequest;
import com.sitemaster.dailyreport.dto.DailyReportResponse;
import com.sitemaster.dailyreport.dto.ReviewDecisionRequest;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileResponses;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/** Daily site reports. */
@RestController
public class DailyReportController {

    private final DailyReportService reportService;

    public DailyReportController(DailyReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/api/daily-reports")
    @PreAuthorize("hasAuthority('DAILY_REPORT:VIEW')")
    public PageResponse<DailyReportResponse> list(@RequestParam(required = false) Long projectId,
                                                  @RequestParam(required = false) RequestStatus status,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @RequestParam(defaultValue = "false") boolean mine,
                                                  @Valid @ModelAttribute PageQuery pageQuery) {
        return reportService.search(projectId, status, from, to, mine, pageQuery);
    }

    @GetMapping("/api/daily-reports/{id}")
    @PreAuthorize("hasAuthority('DAILY_REPORT:VIEW')")
    public DailyReportResponse get(@PathVariable Long id) {
        return reportService.get(id);
    }

    @PostMapping("/api/projects/{projectId}/daily-reports")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('DAILY_REPORT:CREATE')")
    public DailyReportResponse submit(@PathVariable Long projectId, @Valid @RequestBody DailyReportRequest request) {
        return reportService.submit(projectId, request);
    }

    @PutMapping("/api/daily-reports/{id}")
    @PreAuthorize("hasAuthority('DAILY_REPORT:EDIT')")
    public DailyReportResponse update(@PathVariable Long id, @Valid @RequestBody DailyReportRequest request) {
        return reportService.update(id, request);
    }

    @PostMapping(value = "/api/daily-reports/{id}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('DAILY_REPORT:EDIT')")
    public DailyReportResponse addPhoto(@PathVariable Long id,
                                        @RequestPart("file") @ValidFile(category = FileCategory.IMAGE) MultipartFile file,
                                        @RequestParam(value = "caption", required = false)
                                        @Size(max = 150, message = "{caption.size}") String caption) {
        return reportService.addPhoto(id, file, caption);
    }

    @DeleteMapping("/api/daily-reports/{id}/photos/{photoId}")
    @PreAuthorize("hasAuthority('DAILY_REPORT:EDIT')")
    public DailyReportResponse removePhoto(@PathVariable Long id, @PathVariable Long photoId) {
        return reportService.removePhoto(id, photoId);
    }

    @GetMapping("/api/daily-report-photos/{photoId}")
    @PreAuthorize("hasAuthority('DAILY_REPORT:VIEW')")
    public ResponseEntity<Resource> photo(@PathVariable Long photoId) {
        return FileResponses.inline(reportService.openPhoto(photoId));
    }

    @PostMapping("/api/daily-reports/{id}/approve")
    @PreAuthorize("hasAuthority('DAILY_REPORT:APPROVE')")
    public DailyReportResponse approve(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return reportService.approve(id, request.getRemarks());
    }

    @PostMapping("/api/daily-reports/{id}/reject")
    @PreAuthorize("hasAuthority('DAILY_REPORT:APPROVE')")
    public DailyReportResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return reportService.reject(id, request.getRemarks());
    }
}

package com.sitemaster.dailyreport;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.dailyreport.dto.DailyReportRequest;
import com.sitemaster.dailyreport.dto.DailyReportResponse;
import com.sitemaster.file.FileDownload;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

/**
 * Daily site reports: the site engineer submits, the PM approves or rejects.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface DailyReportService {

    PageResponse<DailyReportResponse> search(Long projectId, RequestStatus status, LocalDate from, LocalDate to,
                                             boolean mineOnly, PageQuery pageQuery);

    DailyReportResponse get(Long id);

    DailyReportResponse submit(Long projectId, DailyReportRequest request);

    DailyReportResponse update(Long id, DailyReportRequest request);

    DailyReportResponse addPhoto(Long id, MultipartFile file, String caption);

    DailyReportResponse removePhoto(Long id, Long photoId);

    FileDownload openPhoto(Long photoId);

    DailyReportResponse approve(Long id, String remarks);

    DailyReportResponse reject(Long id, String remarks);
}

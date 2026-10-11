package com.sitemaster.dailyreport;

import com.sitemaster.common.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;

/** Data access for daily reports. */
public interface DailyReportRepository extends JpaRepository<DailyReport, Long>, JpaSpecificationExecutor<DailyReport> {

    boolean existsByProjectIdAndSubmittedByIdAndReportDate(Long projectId, Long userId, LocalDate date);

    long countByProjectIdAndStatus(Long projectId, RequestStatus status);
}

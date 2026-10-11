package com.sitemaster.dailyreport;

import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for daily report photos. */
public interface DailyReportPhotoRepository extends JpaRepository<DailyReportPhoto, Long> {
}

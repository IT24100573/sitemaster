package com.sitemaster.dailyreport.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** A daily report with its materials and photos. */
public record DailyReportResponse(Long id,
                                  Long projectId,
                                  String projectName,
                                  Long submittedById,
                                  String submittedByName,
                                  LocalDate reportDate,
                                  String workDone,
                                  int workerCount,
                                  String weather,
                                  String problems,
                                  String status,
                                  String reviewedByName,
                                  LocalDateTime reviewedAt,
                                  String reviewRemarks,
                                  List<UsedMaterial> materialsUsed,
                                  List<Photo> photos,
                                  boolean canEdit,
                                  boolean canReview,
                                  LocalDateTime createdAt) {

    public DailyReportResponse {
        materialsUsed = List.copyOf(materialsUsed);
        photos = List.copyOf(photos);
    }

    /** A material line. */
    public record UsedMaterial(Long materialId, String materialName, String unit, BigDecimal quantity) {
    }

    /** A photo; download it from /api/daily-report-photos/{id}. */
    public record Photo(Long id, String caption, String fileName) {
    }
}

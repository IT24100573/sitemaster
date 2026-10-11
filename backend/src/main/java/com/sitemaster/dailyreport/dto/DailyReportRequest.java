package com.sitemaster.dailyreport.dto;

import com.sitemaster.common.enums.WeatherCondition;
import com.sitemaster.validation.NotFutureDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Body of a daily report. "Not older than 7 days", "worker count not above the
 * project's workers" and "one per engineer per day" are checked in the service.
 * <p>
 * OOP concept: Encapsulation - private fields, explicit accessors, declarative rules.
 */
public class DailyReportRequest {

    @NotNull(message = "{field.required}")
    @NotFutureDate
    private LocalDate reportDate;

    @NotBlank(message = "{field.required}")
    @Size(min = 10, max = 2000, message = "{dailyReport.workDone.size}")
    private String workDone;

    @NotNull(message = "{field.required}")
    @Min(value = 0, message = "{dailyReport.workers.min}")
    @Max(value = 10000, message = "{dailyReport.workers.min}")
    private Integer workerCount;

    @NotNull(message = "{field.required}")
    private WeatherCondition weather;

    @Size(max = 2000, message = "{text.size}")
    private String problems;

    @Valid
    @Size(max = 30, message = "{dailyReport.materials.size}")
    private List<MaterialUsageRequest> materialsUsed = new ArrayList<>();

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getWorkDone() {
        return workDone;
    }

    public void setWorkDone(String workDone) {
        this.workDone = workDone;
    }

    public Integer getWorkerCount() {
        return workerCount;
    }

    public void setWorkerCount(Integer workerCount) {
        this.workerCount = workerCount;
    }

    public WeatherCondition getWeather() {
        return weather;
    }

    public void setWeather(WeatherCondition weather) {
        this.weather = weather;
    }

    public String getProblems() {
        return problems;
    }

    public void setProblems(String problems) {
        this.problems = problems;
    }

    public List<MaterialUsageRequest> getMaterialsUsed() {
        return materialsUsed;
    }

    public void setMaterialsUsed(List<MaterialUsageRequest> materialsUsed) {
        this.materialsUsed = materialsUsed == null ? new ArrayList<>() : materialsUsed;
    }
}

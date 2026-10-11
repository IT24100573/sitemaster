package com.sitemaster.dailyreport;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.common.enums.RoleName;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.common.exception.UnauthorizedActionException;
import com.sitemaster.dailyreport.dto.DailyReportRequest;
import com.sitemaster.dailyreport.dto.DailyReportResponse;
import com.sitemaster.dailyreport.dto.MaterialUsageRequest;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.material.Material;
import com.sitemaster.material.MaterialRepository;
import com.sitemaster.material.SiteStock;
import com.sitemaster.material.SiteStockRepository;
import com.sitemaster.project.Project;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.project.ProjectMemberRepository;
import com.sitemaster.project.ProjectProgressService;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.user.User;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link DailyReportService}. Rules:
 * <ul>
 *   <li>only a site engineer on the project submits, one report per project per day;</li>
 *   <li>report date not in the future and at most 7 days old;</li>
 *   <li>worker count not above the number of workers on the project team;</li>
 *   <li>materials used cannot exceed the site stock; stock is deducted on approval;</li>
 *   <li>only the project's own PM reviews; approval recalculates project progress.</li>
 * </ul>
 * OOP concept: Abstraction - implements DailyReportService.
 */
@Service
public class DailyReportServiceImpl implements DailyReportService {

    private static final Map<String, String> SORTS = Map.of("reportDate", "reportDate", "createdAt", "createdAt",
            "status", "status");

    private final DailyReportRepository reportRepository;
    private final DailyReportPhotoRepository photoRepository;
    private final MaterialRepository materialRepository;
    private final SiteStockRepository stockRepository;
    private final ProjectMemberRepository memberRepository;
    private final ProjectAccess access;
    private final ProjectProgressService progressService;
    private final FileStorageService fileStorage;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;
    private final Clock clock;

    public DailyReportServiceImpl(DailyReportRepository reportRepository, DailyReportPhotoRepository photoRepository,
                                  MaterialRepository materialRepository, SiteStockRepository stockRepository,
                                  ProjectMemberRepository memberRepository, ProjectAccess access,
                                  ProjectProgressService progressService, FileStorageService fileStorage,
                                  CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher, Clock clock) {
        this.reportRepository = reportRepository;
        this.photoRepository = photoRepository;
        this.materialRepository = materialRepository;
        this.stockRepository = stockRepository;
        this.memberRepository = memberRepository;
        this.access = access;
        this.progressService = progressService;
        this.fileStorage = fileStorage;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DailyReportResponse> search(Long projectId, RequestStatus status, LocalDate from, LocalDate to,
                                                    boolean mineOnly, PageQuery pageQuery) {
        if (projectId != null) {
            access.requireVisible(projectId);
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessRuleException.forField("to", "daterange.invalid");
        }
        Long me = currentUser.requireId();
        String search = pageQuery.getSearch();
        Specification<DailyReport> spec = Specification.where(access.<DailyReport>visibleThrough("project"));
        spec = spec.and((root, q, cb) -> projectId == null ? null : cb.equal(root.get("project").get("id"), projectId))
                .and((root, q, cb) -> status == null ? null : cb.equal(root.get("status"), status))
                .and((root, q, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get("reportDate"), from))
                .and((root, q, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get("reportDate"), to))
                .and((root, q, cb) -> mineOnly ? cb.equal(root.get("submittedBy").get("id"), me) : null)
                .and((root, q, cb) -> search == null ? null
                        : cb.like(cb.lower(root.get("workDone")), "%" + search.toLowerCase(Locale.ROOT) + "%"));
        return PageResponse.from(reportRepository.findAll(spec, pageQuery.toPageable(SORTS, "reportDate", Sort.Direction.DESC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DailyReportResponse get(Long id) {
        DailyReport report = find(id);
        access.requireVisible(report.getProject().getId());
        return toResponse(report);
    }

    @Override
    @Transactional
    public DailyReportResponse submit(Long projectId, DailyReportRequest request) {
        Project project = access.requireParticipant(projectId);
        SiteMasterUserPrincipal me = currentUser.requirePrincipal();
        if (me.getRole() != RoleName.SITE_ENGINEER || !access.isMember(project)) {
            throw new UnauthorizedActionException("dailyReport.submit.denied");
        }
        checkDate(request.getReportDate());
        if (reportRepository.existsByProjectIdAndSubmittedByIdAndReportDate(projectId, me.getUserId(),
                request.getReportDate())) {
            throw DuplicateRecordException.forField("reportDate", "dailyReport.duplicate");
        }
        DailyReport report = new DailyReport(project, currentUser.requireUser(), request.getReportDate());
        fill(report, project, request);
        reportRepository.save(report);

        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DAILY_REPORT_SUBMITTED)
                .entityId(report.getId()).args(project.getName(), report.getReportDate())
                .recipients(project.getManager().getId()).link("daily-reports/" + report.getId()).build());
        return toResponse(report);
    }

    @Override
    @Transactional
    public DailyReportResponse update(Long id, DailyReportRequest request) {
        DailyReport report = requireOwnEditable(id);
        if (!report.getReportDate().equals(request.getReportDate())) {
            throw BusinessRuleException.forField("reportDate", "dailyReport.date.fixed");
        }
        boolean resubmitted = report.getStatus() == RequestStatus.REJECTED;
        fill(report, report.getProject(), request);
        if (resubmitted) {
            eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DAILY_REPORT_SUBMITTED)
                    .entityId(report.getId()).args(report.getProject().getName(), report.getReportDate())
                    .recipients(report.getProject().getManager().getId()).link("daily-reports/" + report.getId())
                    .build());
        }
        return toResponse(report);
    }

    @Override
    @Transactional
    public DailyReportResponse addPhoto(Long id, MultipartFile file, String caption) {
        DailyReport report = requireOwnEditable(id);
        if (report.getPhotos().size() >= DailyReport.MAX_PHOTOS) {
            throw BusinessRuleException.forField("file", "dailyReport.photos.max", DailyReport.MAX_PHOTOS);
        }
        StoredFile stored = fileStorage.store(file, FileCategory.IMAGE, "file");
        report.addPhoto(new DailyReportPhoto(report, stored, caption));
        reportRepository.saveAndFlush(report);
        return toResponse(report);
    }

    @Override
    @Transactional
    public DailyReportResponse removePhoto(Long id, Long photoId) {
        DailyReport report = requireOwnEditable(id);
        DailyReportPhoto photo = report.getPhotos().stream().filter(p -> p.getId().equals(photoId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Photo"));
        StoredFile file = photo.getFile();
        report.removePhoto(photo);
        reportRepository.saveAndFlush(report);
        fileStorage.delete(file);
        return toResponse(report);
    }

    @Override
    @Transactional(readOnly = true)
    public FileDownload openPhoto(Long photoId) {
        DailyReportPhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Photo"));
        access.requireVisible(photo.getReport().getProject().getId());
        return fileStorage.open(photo.getFile());
    }

    @Override
    @Transactional
    public DailyReportResponse approve(Long id, String remarks) {
        DailyReport report = requireReviewable(id);
        report.approve(currentUser.requireUser(), remarks, LocalDateTime.now(clock));
        for (DailyReportMaterial used : report.getMaterialsUsed()) {
            stock(report.getProject(), used.getMaterial()).consume(used.getQuantity());
        }
        int progress = progressService.recalculate(report.getProject().getId());
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DAILY_REPORT_APPROVED)
                .entityId(report.getId()).args(report.getProject().getName(), report.getReportDate(), progress)
                .recipients(report.getSubmittedBy().getId()).link("daily-reports/" + report.getId()).build());
        return toResponse(report);
    }

    @Override
    @Transactional
    public DailyReportResponse reject(Long id, String remarks) {
        DailyReport report = requireReviewable(id);
        report.reject(currentUser.requireUser(), remarks, LocalDateTime.now(clock));
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.DAILY_REPORT_REJECTED)
                .entityId(report.getId()).args(report.getProject().getName(), report.getReportDate(), remarks)
                .recipients(report.getSubmittedBy().getId()).link("daily-reports/" + report.getId()).build());
        return toResponse(report);
    }

    // ----- helpers -------------------------------------------------------------------

    private void fill(DailyReport report, Project project, DailyReportRequest request) {
        long workers = memberRepository.countWorkers(project.getId());
        if (request.getWorkerCount() > workers) {
            throw BusinessRuleException.forField("workerCount", "dailyReport.workers.max", workers);
        }
        report.write(request.getWorkDone(), request.getWorkerCount(), request.getWeather(), request.getProblems());

        Map<Long, BigDecimal> totals = new LinkedHashMap<>();
        for (MaterialUsageRequest line : request.getMaterialsUsed()) {
            totals.merge(line.getMaterialId(), line.getQuantity(), BigDecimal::add);
        }
        List<DailyReportMaterial> items = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : totals.entrySet()) {
            Material material = materialRepository.findById(entry.getKey())
                    .orElseThrow(() -> BusinessRuleException.forField("materialsUsed", "material.unknown"));
            BigDecimal available = stockRepository.findByProjectIdAndMaterialId(project.getId(), material.getId())
                    .map(SiteStock::getQuantity).orElse(BigDecimal.ZERO);
            if (entry.getValue().compareTo(available) > 0) {
                throw BusinessRuleException.forField("materialsUsed", "stock.insufficient", material.getName(), available);
            }
            items.add(new DailyReportMaterial(report, material, entry.getValue()));
        }
        report.replaceMaterials(items);
    }

    private void checkDate(LocalDate date) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today.minusDays(DailyReport.MAX_AGE_DAYS))) {
            throw BusinessRuleException.forField("reportDate", "dailyReport.date.tooOld", DailyReport.MAX_AGE_DAYS);
        }
    }

    private SiteStock stock(Project project, Material material) {
        return stockRepository.findByProjectIdAndMaterialId(project.getId(), material.getId())
                .orElseGet(() -> stockRepository.save(new SiteStock(project, material)));
    }

    private DailyReport requireOwnEditable(Long id) {
        DailyReport report = find(id);
        access.requireParticipant(report.getProject().getId());
        if (!report.isAuthor(currentUser.requireId())) {
            throw new UnauthorizedActionException("dailyReport.edit.denied");
        }
        if (!report.isEditable()) {
            throw new BusinessRuleException("dailyReport.approved");
        }
        return report;
    }

    /** Only the project's own PM reviews daily reports. */
    private DailyReport requireReviewable(Long id) {
        DailyReport report = find(id);
        Project project = access.requireParticipant(report.getProject().getId());
        if (!project.isManagedBy(currentUser.requireId())) {
            throw new UnauthorizedActionException("dailyReport.review.denied");
        }
        return report;
    }

    private DailyReport find(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Daily report"));
    }

    private DailyReportResponse toResponse(DailyReport r) {
        Long me = currentUser.requireId();
        boolean canEdit = r.isEditable() && r.isAuthor(me);
        boolean canReview = r.getStatus() == RequestStatus.PENDING && r.getProject().isManagedBy(me) && !r.isAuthor(me);
        User reviewer = r.getReviewedBy();
        return new DailyReportResponse(r.getId(), r.getProject().getId(), r.getProject().getName(),
                r.getSubmittedBy().getId(), r.getSubmittedBy().getFullName(), r.getReportDate(), r.getWorkDone(),
                r.getWorkerCount(), r.getWeather().name(), r.getProblems(), r.getStatus().name(),
                reviewer == null ? null : reviewer.getFullName(), r.getReviewedAt(), r.getReviewRemarks(),
                r.getMaterialsUsed().stream().map(m -> new DailyReportResponse.UsedMaterial(m.getMaterial().getId(),
                        m.getMaterial().getName(), m.getMaterial().getUnit(), m.getQuantity())).toList(),
                r.getPhotos().stream().map(p -> new DailyReportResponse.Photo(p.getId(), p.getCaption(),
                        p.getFile().getOriginalName())).toList(),
                canEdit, canReview, r.getCreatedAt());
    }
}

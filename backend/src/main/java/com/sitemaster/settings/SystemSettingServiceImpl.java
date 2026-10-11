package com.sitemaster.settings;

import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.file.FileCategory;
import com.sitemaster.file.FileDownload;
import com.sitemaster.file.FileStorageService;
import com.sitemaster.file.StoredFile;
import com.sitemaster.file.StoredFileRepository;
import com.sitemaster.security.CurrentUserProvider;
import com.sitemaster.security.SiteMasterUserPrincipal;
import com.sitemaster.settings.dto.CompanyProfileResponse;
import com.sitemaster.settings.dto.SystemSettingsRequest;
import com.sitemaster.settings.dto.SystemSettingsResponse;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of {@link SystemSettingService}. After each save the
 * SystemSettingsManager singleton is reloaded so the new values apply at once.
 * <p>
 * OOP concept: Abstraction - implements SystemSettingService.
 */
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final SystemSettingRepository repository;
    private final SystemSettingsLoader loader;
    private final FileStorageService fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final CurrentUserProvider currentUser;
    private final WorkflowEventPublisher eventPublisher;

    public SystemSettingServiceImpl(SystemSettingRepository repository, SystemSettingsLoader loader,
                                    FileStorageService fileStorage, StoredFileRepository storedFileRepository,
                                    CurrentUserProvider currentUser, WorkflowEventPublisher eventPublisher) {
        this.repository = repository;
        this.loader = loader;
        this.fileStorage = fileStorage;
        this.storedFileRepository = storedFileRepository;
        this.currentUser = currentUser;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public SystemSettingsResponse getSettings() {
        return toResponse(SystemSettingsManager.getInstance().getSettings());
    }

    @Override
    @Transactional
    public SystemSettingsResponse updateSettings(SystemSettingsRequest request) {
        if (!request.getWorkStartTime().isBefore(request.getWorkEndTime())) {
            throw BusinessRuleException.forField("workEndTime", "settings.workingHours.order");
        }
        LinkedHashSet<String> trades = request.getTrades().stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<SettingKey, String> values = new LinkedHashMap<>();
        values.put(SettingKey.COMPANY_NAME, request.getCompanyName());
        values.put(SettingKey.COMPANY_DESCRIPTION, nullToEmpty(request.getCompanyDescription()));
        values.put(SettingKey.COMPANY_ADDRESS, request.getCompanyAddress());
        values.put(SettingKey.COMPANY_PHONE, request.getCompanyPhone());
        values.put(SettingKey.COMPANY_EMAIL, request.getCompanyEmail().toLowerCase());
        values.put(SettingKey.CURRENCY, request.getCurrency());
        values.put(SettingKey.RECEIPT_THRESHOLD, money(request.getReceiptThreshold()));
        values.put(SettingKey.WORK_START_TIME, request.getWorkStartTime().format(TIME));
        values.put(SettingKey.WORK_END_TIME, request.getWorkEndTime().format(TIME));
        values.put(SettingKey.HOLIDAYS, request.getHolidays().stream().distinct().sorted()
                .map(LocalDate::toString).collect(Collectors.joining(CompanySettings.LIST_SEPARATOR)));
        values.put(SettingKey.LEAVE_ANNUAL_DAYS, String.valueOf(request.getLeaveAnnualDays()));
        values.put(SettingKey.LEAVE_CASUAL_DAYS, String.valueOf(request.getLeaveCasualDays()));
        values.put(SettingKey.LEAVE_SICK_DAYS, String.valueOf(request.getLeaveSickDays()));
        values.put(SettingKey.EPF_EMPLOYEE_RATE, money(request.getEpfEmployeeRate()));
        values.put(SettingKey.EPF_EMPLOYER_RATE, money(request.getEpfEmployerRate()));
        values.put(SettingKey.ETF_RATE, money(request.getEtfRate()));
        values.put(SettingKey.OVERTIME_MULTIPLIER, money(request.getOvertimeMultiplier()));
        values.put(SettingKey.SITE_ALLOWANCE_PER_DAY, money(request.getSiteAllowancePerDay()));
        values.put(SettingKey.EMPLOYEE_TRADES, String.join(CompanySettings.LIST_SEPARATOR, trades));
        values.put(SettingKey.MATERIAL_CATEGORIES, String.join(CompanySettings.LIST_SEPARATOR,
                request.getMaterialCategories().stream().map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new))));
        values.put(SettingKey.MATERIAL_UNITS, String.join(CompanySettings.LIST_SEPARATOR,
                request.getMaterialUnits().stream().map(String::trim)
                        .collect(Collectors.toCollection(LinkedHashSet::new))));

        values.forEach(this::save);
        loader.reload();
        publish(WorkflowEventType.SETTINGS_UPDATED);
        return getSettings();
    }

    @Override
    @Transactional
    public SystemSettingsResponse updateLogo(MultipartFile file) {
        StoredFile stored = fileStorage.store(file, FileCategory.IMAGE, "file");
        Optional<StoredFile> previous = findLogo();
        save(SettingKey.COMPANY_LOGO_FILE_ID, String.valueOf(stored.getId()));
        previous.ifPresent(fileStorage::delete);
        loader.reload();
        publish(WorkflowEventType.COMPANY_LOGO_UPDATED);
        return getSettings();
    }

    @Override
    @Transactional
    public SystemSettingsResponse removeLogo() {
        Optional<StoredFile> previous = findLogo();
        save(SettingKey.COMPANY_LOGO_FILE_ID, "");
        previous.ifPresent(fileStorage::delete);
        loader.reload();
        publish(WorkflowEventType.COMPANY_LOGO_UPDATED);
        return getSettings();
    }

    @Override
    public CompanyProfileResponse getCompanyProfile() {
        CompanySettings s = SystemSettingsManager.getInstance().getSettings();
        return new CompanyProfileResponse(s.companyName(), s.companyDescription(), s.companyAddress(),
                s.companyPhone(), s.companyEmail(), s.currency(), s.hasLogo());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FileDownload> openLogo() {
        return findLogo().map(fileStorage::open);
    }

    private Optional<StoredFile> findLogo() {
        Long id = SystemSettingsManager.getInstance().getSettings().companyLogoFileId();
        return id == null ? Optional.empty() : storedFileRepository.findById(id);
    }

    private void save(SettingKey key, String value) {
        SystemSetting setting = repository.findByKey(key.getKey())
                .orElseGet(() -> new SystemSetting(key.getKey(), "", null));
        setting.setValue(value);
        repository.save(setting);
    }

    private void publish(WorkflowEventType type) {
        SiteMasterUserPrincipal actor = currentUser.requirePrincipal();
        eventPublisher.publish(WorkflowEvent.builder(type).actor(actor.getUserId(), actor.getFullName()).build());
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static SystemSettingsResponse toResponse(CompanySettings s) {
        return new SystemSettingsResponse(s.companyName(), s.companyDescription(), s.companyAddress(),
                s.companyPhone(), s.companyEmail(), s.hasLogo(), s.currency(), s.receiptThreshold(),
                s.workStartTime(), s.workEndTime(), s.holidays(), s.leaveAnnualDays(), s.leaveCasualDays(),
                s.leaveSickDays(), s.epfEmployeeRate(), s.epfEmployerRate(), s.etfRate(),
                s.overtimeMultiplier(), s.siteAllowancePerDay(), s.trades(), s.materialCategories(), s.materialUnits());
    }
}

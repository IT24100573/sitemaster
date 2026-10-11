package com.sitemaster.settings;

import com.sitemaster.file.FileCategory;
import com.sitemaster.settings.dto.SystemSettingsRequest;
import com.sitemaster.settings.dto.SystemSettingsResponse;
import com.sitemaster.validation.ValidFile;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Admin: System Settings page. */
@RestController
@RequestMapping("/api/admin/settings")
public class SettingsController {

    private final SystemSettingService settingService;

    public SettingsController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SETTINGS:VIEW')")
    public SystemSettingsResponse get() {
        return settingService.getSettings();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('SETTINGS:EDIT')")
    public SystemSettingsResponse update(@Valid @RequestBody SystemSettingsRequest request) {
        return settingService.updateSettings(request);
    }

    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SETTINGS:EDIT')")
    public SystemSettingsResponse uploadLogo(@RequestPart("file") @ValidFile(category = FileCategory.IMAGE) MultipartFile file) {
        return settingService.updateLogo(file);
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasAuthority('SETTINGS:EDIT')")
    public SystemSettingsResponse removeLogo() {
        return settingService.removeLogo();
    }
}

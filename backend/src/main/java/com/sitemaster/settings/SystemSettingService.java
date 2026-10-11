package com.sitemaster.settings;

import com.sitemaster.file.FileDownload;
import com.sitemaster.settings.dto.CompanyProfileResponse;
import com.sitemaster.settings.dto.SystemSettingsRequest;
import com.sitemaster.settings.dto.SystemSettingsResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Reading and changing system settings.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface SystemSettingService {

    SystemSettingsResponse getSettings();

    SystemSettingsResponse updateSettings(SystemSettingsRequest request);

    SystemSettingsResponse updateLogo(MultipartFile file);

    SystemSettingsResponse removeLogo();

    CompanyProfileResponse getCompanyProfile();

    /** The company logo, if one has been uploaded. */
    Optional<FileDownload> openLogo();
}

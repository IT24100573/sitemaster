package com.sitemaster.settings;

import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.file.FileResponses;
import com.sitemaster.settings.dto.CompanyProfileResponse;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public company profile and logo (About Us page, header branding). */
@RestController
@RequestMapping("/api/public/company")
public class PublicCompanyController {

    private final SystemSettingService settingService;

    public PublicCompanyController(SystemSettingService settingService) {
        this.settingService = settingService;
    }

    @GetMapping
    public CompanyProfileResponse profile() {
        return settingService.getCompanyProfile();
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> logo() {
        return settingService.openLogo()
                .map(FileResponses::inline)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Logo"));
    }
}

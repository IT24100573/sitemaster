package com.sitemaster.lookup;

import com.sitemaster.common.dto.LookupOption;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dropdown data needed by public pages. */
@RestController
@RequestMapping("/api/public/lookups")
public class PublicLookupController {

    private final LookupService lookupService;

    public PublicLookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/material-categories")
    public List<LookupOption> materialCategories() {
        return lookupService.materialCategories();
    }
}

package com.sitemaster.supplier;

import com.sitemaster.common.dto.MessageResponse;
import com.sitemaster.supplier.dto.SupplierApplicationForm;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Public "Become a Supplier" form submission. */
@RestController
@RequestMapping("/api/public/supplier-applications")
public class PublicSupplierApplicationController {

    private final SupplierApplicationService applicationService;

    public PublicSupplierApplicationController(SupplierApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse submit(@Valid @ModelAttribute SupplierApplicationForm form) {
        return applicationService.submit(form);
    }
}

package com.sitemaster.supplier;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.supplier.dto.SupplierResponse;
import com.sitemaster.user.dto.UserStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Approved suppliers (Admin manages; Accountant views for purchase orders). */
@RestController
@RequestMapping("/api/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER:VIEW')")
    public PageResponse<SupplierResponse> list(@RequestParam(required = false) Boolean active,
                                               @RequestParam(required = false)
                                               @Size(max = 50, message = "{trade.size}") String category,
                                               @Valid @ModelAttribute PageQuery pageQuery) {
        return supplierService.search(active, category, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER:VIEW')")
    public SupplierResponse get(@PathVariable Long id) {
        return supplierService.get(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SUPPLIER:EDIT')")
    public SupplierResponse setActive(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return supplierService.setActive(id, request.getActive());
    }
}

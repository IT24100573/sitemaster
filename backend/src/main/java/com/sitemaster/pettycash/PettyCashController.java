package com.sitemaster.pettycash;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.pettycash.dto.AdjustmentRequest;
import com.sitemaster.pettycash.dto.FundRequest;
import com.sitemaster.pettycash.dto.FundResponse;
import com.sitemaster.pettycash.dto.TopUpRequest;
import com.sitemaster.pettycash.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Project petty cash. */
@RestController
public class PettyCashController {

    private final PettyCashService service;

    public PettyCashController(PettyCashService service) {
        this.service = service;
    }

    @GetMapping("/api/petty-cash")
    @PreAuthorize("hasAuthority('EXPENSE:VIEW')")
    public List<FundResponse> funds() {
        return service.funds();
    }

    @GetMapping("/api/projects/{projectId}/petty-cash")
    @PreAuthorize("hasAuthority('EXPENSE:VIEW')")
    public FundResponse fund(@PathVariable Long projectId) {
        return service.fund(projectId);
    }

    @PutMapping("/api/projects/{projectId}/petty-cash")
    @PreAuthorize("hasAuthority('PAYMENT:CREATE')")
    public FundResponse saveFund(@PathVariable Long projectId, @Valid @RequestBody FundRequest request) {
        return service.saveFund(projectId, request);
    }

    @PostMapping("/api/projects/{projectId}/petty-cash/top-ups")
    @PreAuthorize("hasAuthority('PAYMENT:CREATE')")
    public FundResponse topUp(@PathVariable Long projectId, @Valid @RequestBody TopUpRequest request) {
        return service.topUp(projectId, request);
    }

    @PostMapping("/api/projects/{projectId}/petty-cash/adjustments")
    @PreAuthorize("hasAuthority('PAYMENT:EDIT')")
    public FundResponse adjust(@PathVariable Long projectId, @Valid @RequestBody AdjustmentRequest request) {
        return service.adjust(projectId, request);
    }

    @GetMapping("/api/projects/{projectId}/petty-cash/transactions")
    @PreAuthorize("hasAuthority('EXPENSE:VIEW')")
    public PageResponse<TransactionResponse> transactions(@PathVariable Long projectId,
                                                          @Valid @ModelAttribute PageQuery pageQuery) {
        return service.transactions(projectId, pageQuery);
    }
}

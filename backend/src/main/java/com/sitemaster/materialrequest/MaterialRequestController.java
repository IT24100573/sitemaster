package com.sitemaster.materialrequest;

import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.dailyreport.dto.ReviewDecisionRequest;
import com.sitemaster.materialrequest.dto.DeliveryRequest;
import com.sitemaster.materialrequest.dto.MaterialRequestCreate;
import com.sitemaster.materialrequest.dto.MaterialRequestResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Material requests. */
@RestController
public class MaterialRequestController {

    private final MaterialRequestService requestService;

    public MaterialRequestController(MaterialRequestService requestService) {
        this.requestService = requestService;
    }

    @GetMapping("/api/material-requests")
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:VIEW')")
    public PageResponse<MaterialRequestResponse> list(@RequestParam(required = false) Long projectId,
                                                      @RequestParam(required = false) RequestStatus status,
                                                      @RequestParam(defaultValue = "false") boolean mine,
                                                      @Valid @ModelAttribute PageQuery pageQuery) {
        return requestService.search(projectId, status, mine, pageQuery);
    }

    @GetMapping("/api/material-requests/{id}")
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:VIEW')")
    public MaterialRequestResponse get(@PathVariable Long id) {
        return requestService.get(id);
    }

    @PostMapping("/api/projects/{projectId}/material-requests")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:CREATE')")
    public MaterialRequestResponse create(@PathVariable Long projectId, @Valid @RequestBody MaterialRequestCreate request) {
        return requestService.create(projectId, request);
    }

    @GetMapping("/api/material-requests/{id}/budget-check")
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:APPROVE')")
    public BudgetCheckResponse budgetCheck(@PathVariable Long id) {
        return requestService.budgetCheck(id);
    }

    @PostMapping("/api/material-requests/{id}/approve")
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:APPROVE')")
    public MaterialRequestResponse approve(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return requestService.approve(id, request.getRemarks(), request.isAcknowledgeOverrun());
    }

    @PostMapping("/api/material-requests/{id}/reject")
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:APPROVE')")
    public MaterialRequestResponse reject(@PathVariable Long id, @Valid @RequestBody ReviewDecisionRequest request) {
        return requestService.reject(id, request.getRemarks());
    }

    @PostMapping("/api/material-requests/{id}/delivery")
    @PreAuthorize("hasAuthority('MATERIAL_REQUEST:EDIT')")
    public MaterialRequestResponse confirmDelivery(@PathVariable Long id, @Valid @RequestBody DeliveryRequest request) {
        return requestService.confirmDelivery(id, request);
    }
}

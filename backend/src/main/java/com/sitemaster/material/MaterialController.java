package com.sitemaster.material;

import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.material.dto.MaterialRequestBody;
import com.sitemaster.material.dto.MaterialResponse;
import com.sitemaster.material.dto.StockResponse;
import com.sitemaster.user.dto.UserStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Material catalogue and site stock. */
@RestController
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/api/materials")
    @PreAuthorize("hasAuthority('MATERIAL:VIEW')")
    public PageResponse<MaterialResponse> list(@RequestParam(required = false) @Size(max = 50, message = "{trade.size}") String category,
                                               @RequestParam(required = false) Boolean active,
                                               @Valid @ModelAttribute PageQuery pageQuery) {
        return materialService.search(category, active, pageQuery);
    }

    @GetMapping("/api/materials/options")
    @PreAuthorize("hasAnyAuthority('MATERIAL:VIEW', 'MATERIAL_REQUEST:CREATE', 'DAILY_REPORT:CREATE')")
    public List<LookupOption> options() {
        return materialService.activeOptions();
    }

    @PostMapping("/api/materials")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('MATERIAL:CREATE')")
    public MaterialResponse create(@Valid @RequestBody MaterialRequestBody request) {
        return materialService.create(request);
    }

    @PutMapping("/api/materials/{id}")
    @PreAuthorize("hasAuthority('MATERIAL:EDIT')")
    public MaterialResponse update(@PathVariable Long id, @Valid @RequestBody MaterialRequestBody request) {
        return materialService.update(id, request);
    }

    @PatchMapping("/api/materials/{id}/status")
    @PreAuthorize("hasAuthority('MATERIAL:EDIT')")
    public MaterialResponse setActive(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return materialService.setActive(id, request.getActive());
    }

    @GetMapping("/api/projects/{projectId}/stock")
    @PreAuthorize("hasAuthority('MATERIAL:VIEW')")
    public List<StockResponse> stock(@PathVariable Long projectId) {
        return materialService.stock(projectId);
    }
}

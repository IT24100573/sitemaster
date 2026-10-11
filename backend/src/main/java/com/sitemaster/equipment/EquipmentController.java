package com.sitemaster.equipment;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.equipment.dto.EquipmentAssignmentRequest;
import com.sitemaster.equipment.dto.EquipmentConditionRequest;
import com.sitemaster.equipment.dto.EquipmentRequest;
import com.sitemaster.equipment.dto.EquipmentResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Equipment register. */
@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('EQUIPMENT:VIEW')")
    public PageResponse<EquipmentResponse> list(@RequestParam(required = false) Long projectId,
                                                @RequestParam(required = false) EquipmentStatus status,
                                                @Valid @ModelAttribute PageQuery pageQuery) {
        return equipmentService.search(projectId, status, pageQuery);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('EQUIPMENT:CREATE')")
    public EquipmentResponse create(@Valid @RequestBody EquipmentRequest request) {
        return equipmentService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('EQUIPMENT:CREATE')")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return equipmentService.update(id, request);
    }

    @PatchMapping("/{id}/assignment")
    @PreAuthorize("hasAuthority('EQUIPMENT:EDIT')")
    public EquipmentResponse assign(@PathVariable Long id, @RequestBody EquipmentAssignmentRequest request) {
        return equipmentService.assign(id, request.getProjectId());
    }

    @PatchMapping("/{id}/condition")
    @PreAuthorize("hasAuthority('EQUIPMENT:EDIT')")
    public EquipmentResponse updateCondition(@PathVariable Long id, @Valid @RequestBody EquipmentConditionRequest request) {
        return equipmentService.updateCondition(id, request);
    }
}

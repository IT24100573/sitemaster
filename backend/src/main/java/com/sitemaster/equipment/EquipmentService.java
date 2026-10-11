package com.sitemaster.equipment;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.EquipmentStatus;
import com.sitemaster.equipment.dto.EquipmentConditionRequest;
import com.sitemaster.equipment.dto.EquipmentRequest;
import com.sitemaster.equipment.dto.EquipmentResponse;

/**
 * Equipment register.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface EquipmentService {

    PageResponse<EquipmentResponse> search(Long projectId, EquipmentStatus status, PageQuery pageQuery);

    EquipmentResponse create(EquipmentRequest request);

    EquipmentResponse update(Long id, EquipmentRequest request);

    EquipmentResponse assign(Long id, Long projectId);

    EquipmentResponse updateCondition(Long id, EquipmentConditionRequest request);
}

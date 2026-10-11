package com.sitemaster.attendance;

import com.sitemaster.attendance.dto.ShiftRequest;
import com.sitemaster.attendance.dto.ShiftResponse;

import java.util.List;

/**
 * Working shifts.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ShiftService {

    List<ShiftResponse> list();

    ShiftResponse create(ShiftRequest request);

    ShiftResponse update(Long id, ShiftRequest request);
}

package com.sitemaster.attendance;

import com.sitemaster.attendance.dto.ShiftRequest;
import com.sitemaster.attendance.dto.ShiftResponse;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link ShiftService}: names are unique, end after start.
 * <p>
 * OOP concept: Abstraction - implements ShiftService.
 */
@Service
public class ShiftServiceImpl implements ShiftService {

    private final ShiftRepository shiftRepository;
    private final WorkflowEventPublisher eventPublisher;

    public ShiftServiceImpl(ShiftRepository shiftRepository, WorkflowEventPublisher eventPublisher) {
        this.shiftRepository = shiftRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShiftResponse> list() {
        return shiftRepository.findAllByOrderByStartTimeAsc().stream().map(ShiftServiceImpl::toResponse).toList();
    }

    @Override
    @Transactional
    public ShiftResponse create(ShiftRequest request) {
        if (shiftRepository.existsByNameIgnoreCase(request.getName())) {
            throw DuplicateRecordException.forField("name", "shift.name.duplicate");
        }
        Shift shift = new Shift(request.getName());
        apply(shift, request);
        shiftRepository.save(shift);
        publish(shift);
        return toResponse(shift);
    }

    @Override
    @Transactional
    public ShiftResponse update(Long id, ShiftRequest request) {
        Shift shift = shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Shift"));
        if (shiftRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw DuplicateRecordException.forField("name", "shift.name.duplicate");
        }
        apply(shift, request);
        publish(shift);
        return toResponse(shift);
    }

    private void apply(Shift shift, ShiftRequest r) {
        shift.update(r.getName(), r.getStartTime(), r.getEndTime(), r.getDescription());
        shift.setActive(r.isActive());
    }

    private void publish(Shift shift) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.SHIFT_SAVED)
                .entityId(shift.getId()).args(shift.getName()).build());
    }

    private static ShiftResponse toResponse(Shift s) {
        return new ShiftResponse(s.getId(), s.getName(), s.getStartTime(), s.getEndTime(), s.getDescription(), s.isActive());
    }
}

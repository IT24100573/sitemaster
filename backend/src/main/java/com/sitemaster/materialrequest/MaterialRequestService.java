package com.sitemaster.materialrequest;

import com.sitemaster.budget.dto.BudgetCheckResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.RequestStatus;
import com.sitemaster.materialrequest.dto.DeliveryRequest;
import com.sitemaster.materialrequest.dto.MaterialRequestCreate;
import com.sitemaster.materialrequest.dto.MaterialRequestResponse;

/**
 * Material requests: create (site engineer), approve/reject (project's PM),
 * confirm delivery (site engineer). Ordering and paying live in the purchase module.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface MaterialRequestService {

    PageResponse<MaterialRequestResponse> search(Long projectId, RequestStatus status, boolean mineOnly,
                                                 PageQuery pageQuery);

    MaterialRequestResponse get(Long id);

    MaterialRequestResponse create(Long projectId, MaterialRequestCreate request);

    BudgetCheckResponse budgetCheck(Long id);

    MaterialRequestResponse approve(Long id, String remarks, boolean acknowledgeOverrun);

    MaterialRequestResponse reject(Long id, String remarks);

    MaterialRequestResponse confirmDelivery(Long id, DeliveryRequest request);
}

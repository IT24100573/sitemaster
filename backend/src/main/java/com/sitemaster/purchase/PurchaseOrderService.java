package com.sitemaster.purchase;

import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.enums.PurchaseOrderStatus;
import com.sitemaster.purchase.dto.CreatePurchaseOrderRequest;
import com.sitemaster.purchase.dto.InvoiceUploadForm;
import com.sitemaster.purchase.dto.PurchaseOrderResponse;

import java.time.LocalDate;

/**
 * Purchase orders: raised by the Accountant, answered and fulfilled by the
 * addressed supplier. Supplier methods always act on the signed-in supplier's orders.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface PurchaseOrderService {

    PageResponse<PurchaseOrderResponse> search(Long projectId, PurchaseOrderStatus status, PageQuery pageQuery);

    PurchaseOrderResponse get(Long id);

    PurchaseOrderResponse create(Long materialRequestId, CreatePurchaseOrderRequest request);

    PageResponse<PurchaseOrderResponse> supplierOrders(PurchaseOrderStatus status, PageQuery pageQuery);

    PurchaseOrderResponse supplierOrder(Long id);

    PurchaseOrderResponse accept(Long id);

    PurchaseOrderResponse decline(Long id, String reason);

    PurchaseOrderResponse dispatch(Long id, LocalDate expectedDeliveryDate);

    PurchaseOrderResponse uploadInvoice(Long id, InvoiceUploadForm form);
}

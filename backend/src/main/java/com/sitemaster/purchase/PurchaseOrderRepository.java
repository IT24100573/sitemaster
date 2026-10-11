package com.sitemaster.purchase;

import com.sitemaster.common.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

/** Data access for purchase orders. */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    @Query("select p from PurchaseOrder p where p.materialRequest.id = :requestId "
            + "and p.status <> com.sitemaster.common.enums.PurchaseOrderStatus.DECLINED")
    Optional<PurchaseOrder> findActiveForRequest(@Param("requestId") Long requestId);

    long countByProjectIdAndStatusIn(Long projectId, Collection<PurchaseOrderStatus> statuses);

    @Query(value = "select nextval('purchase_order_number_seq')", nativeQuery = true)
    long nextNumber();
}

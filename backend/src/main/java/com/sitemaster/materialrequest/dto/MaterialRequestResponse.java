package com.sitemaster.materialrequest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** A material request with its current purchase order, if any. */
public record MaterialRequestResponse(Long id,
                                      Long projectId,
                                      String projectName,
                                      Long phaseId,
                                      String phaseName,
                                      Long materialId,
                                      String materialName,
                                      String unit,
                                      BigDecimal quantity,
                                      String urgency,
                                      LocalDate requiredBy,
                                      BigDecimal estimatedCost,
                                      String notes,
                                      String status,
                                      Long requestedById,
                                      String requestedByName,
                                      String approvedByName,
                                      String remarks,
                                      LocalDateTime decidedAt,
                                      BigDecimal deliveredQuantity,
                                      LocalDateTime deliveredAt,
                                      String deliveredByName,
                                      String deliveryNotes,
                                      Long purchaseOrderId,
                                      String purchaseOrderNumber,
                                      String purchaseOrderStatus,
                                      boolean canReview,
                                      boolean canConfirmDelivery,
                                      LocalDateTime createdAt) {
}

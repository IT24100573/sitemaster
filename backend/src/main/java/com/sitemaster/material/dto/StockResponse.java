package com.sitemaster.material.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Quantity of a material at a site. */
public record StockResponse(Long materialId, String materialName, String category, String unit,
                            BigDecimal quantity, LocalDateTime updatedAt) {
}

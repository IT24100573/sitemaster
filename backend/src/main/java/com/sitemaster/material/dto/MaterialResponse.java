package com.sitemaster.material.dto;

import java.math.BigDecimal;

/** A catalogue material. */
public record MaterialResponse(Long id, String name, String category, String unit, BigDecimal unitPrice,
                               String description, boolean active) {
}

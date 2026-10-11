package com.sitemaster.project.dto;

import java.math.BigDecimal;

/** One marker on the public Sites Map. */
public record ProjectMapMarker(Long id,
                               String name,
                               String status,
                               int progress,
                               String location,
                               BigDecimal latitude,
                               BigDecimal longitude) {
}

package com.sitemaster.pettycash.dto;

import java.math.BigDecimal;

/** A petty cash fund; {@code canManage} is true for the Accountant. */
public record FundResponse(Long id, Long projectId, String projectName, Long custodianId, String custodianName,
                           BigDecimal floatLimit, BigDecimal balance, boolean active, boolean canManage) {
}

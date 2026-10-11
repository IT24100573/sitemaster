package com.sitemaster.client.dto;

import java.time.LocalDateTime;

/**
 * A client as shown on the Clients page.
 */
public record ClientResponse(Long id,
                             String name,
                             String contactPerson,
                             String email,
                             String phone,
                             String address,
                             boolean active,
                             long projectCount,
                             LocalDateTime createdAt) {
}

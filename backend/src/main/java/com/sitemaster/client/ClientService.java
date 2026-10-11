package com.sitemaster.client;

import com.sitemaster.client.dto.ClientRequest;
import com.sitemaster.client.dto.ClientResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;

/**
 * Client records.
 * <p>
 * OOP concept: Abstraction - controllers depend on this interface.
 */
public interface ClientService {

    PageResponse<ClientResponse> search(Boolean active, PageQuery pageQuery);

    ClientResponse get(Long id);

    ClientResponse create(ClientRequest request);

    ClientResponse update(Long id, ClientRequest request);

    ClientResponse setActive(Long id, boolean active);
}

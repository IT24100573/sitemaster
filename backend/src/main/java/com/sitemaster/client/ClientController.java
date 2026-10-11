package com.sitemaster.client;

import com.sitemaster.client.dto.ClientRequest;
import com.sitemaster.client.dto.ClientResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.user.dto.UserStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Clients (Admin manages; Accountant uses them for billing). */
@RestController
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CLIENT:VIEW')")
    public PageResponse<ClientResponse> list(@RequestParam(required = false) Boolean active,
                                             @Valid @ModelAttribute PageQuery pageQuery) {
        return clientService.search(active, pageQuery);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT:VIEW')")
    public ClientResponse get(@PathVariable Long id) {
        return clientService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('CLIENT:CREATE')")
    public ClientResponse create(@Valid @RequestBody ClientRequest request) {
        return clientService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CLIENT:EDIT')")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return clientService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('CLIENT:EDIT')")
    public ClientResponse setActive(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        return clientService.setActive(id, request.getActive());
    }
}

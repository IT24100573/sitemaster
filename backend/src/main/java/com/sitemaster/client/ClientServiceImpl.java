package com.sitemaster.client;

import com.sitemaster.client.dto.ClientRequest;
import com.sitemaster.client.dto.ClientResponse;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.project.ProjectRepository;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link ClientService}.
 * <p>
 * OOP concept: Abstraction - implements ClientService.
 */
@Service
public class ClientServiceImpl implements ClientService {

    private static final Map<String, String> SORTS = Map.of("name", "name", "createdAt", "createdAt");

    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final WorkflowEventPublisher eventPublisher;

    public ClientServiceImpl(ClientRepository clientRepository, ProjectRepository projectRepository,
                             WorkflowEventPublisher eventPublisher) {
        this.clientRepository = clientRepository;
        this.projectRepository = projectRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClientResponse> search(Boolean active, PageQuery pageQuery) {
        String search = pageQuery.getSearch();
        Specification<Client> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            if (search != null) {
                String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("email")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("contactPerson"), "")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(clientRepository.findAll(spec, pageQuery.toPageable(SORTS, "name", Sort.Direction.ASC)),
                this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = new Client(request.getName());
        apply(client, request);
        clientRepository.save(client);
        publish(WorkflowEventType.CLIENT_CREATED, client);
        return toResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse update(Long id, ClientRequest request) {
        Client client = find(id);
        apply(client, request);
        publish(WorkflowEventType.CLIENT_UPDATED, client);
        return toResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse setActive(Long id, boolean active) {
        Client client = find(id);
        if (active) {
            client.activate();
        } else {
            client.deactivate();
        }
        publish(WorkflowEventType.CLIENT_UPDATED, client);
        return toResponse(client);
    }

    private void apply(Client client, ClientRequest r) {
        client.updateDetails(r.getName(), r.getContactPerson(), r.getEmail(), r.getPhone(), r.getAddress());
    }

    private void publish(WorkflowEventType type, Client client) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(type).entityId(client.getId()).args(client.getName()).build());
    }

    private Client find(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Client"));
    }

    private ClientResponse toResponse(Client c) {
        return new ClientResponse(c.getId(), c.getName(), c.getContactPerson(), c.getEmail(), c.getPhone(),
                c.getAddress(), c.isActive(), projectRepository.countByClientId(c.getId()), c.getCreatedAt());
    }
}

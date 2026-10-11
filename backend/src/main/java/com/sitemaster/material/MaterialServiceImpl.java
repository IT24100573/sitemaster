package com.sitemaster.material;

import com.sitemaster.common.dto.LookupOption;
import com.sitemaster.common.dto.PageQuery;
import com.sitemaster.common.dto.PageResponse;
import com.sitemaster.common.exception.BusinessRuleException;
import com.sitemaster.common.exception.DuplicateRecordException;
import com.sitemaster.common.exception.ResourceNotFoundException;
import com.sitemaster.material.dto.MaterialRequestBody;
import com.sitemaster.material.dto.MaterialResponse;
import com.sitemaster.material.dto.StockResponse;
import com.sitemaster.project.ProjectAccess;
import com.sitemaster.settings.CompanySettings;
import com.sitemaster.settings.SystemSettingsManager;
import com.sitemaster.workflow.WorkflowEvent;
import com.sitemaster.workflow.WorkflowEventPublisher;
import com.sitemaster.workflow.WorkflowEventType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Implementation of {@link MaterialService}. Names are unique; category and
 * unit must come from System Settings.
 * <p>
 * OOP concept: Abstraction - implements MaterialService.
 */
@Service
public class MaterialServiceImpl implements MaterialService {

    private static final Map<String, String> SORTS = Map.of(
            "name", "name", "category", "category", "unitPrice", "unitPrice");

    private final MaterialRepository materialRepository;
    private final SiteStockRepository stockRepository;
    private final ProjectAccess access;
    private final WorkflowEventPublisher eventPublisher;

    public MaterialServiceImpl(MaterialRepository materialRepository, SiteStockRepository stockRepository,
                               ProjectAccess access, WorkflowEventPublisher eventPublisher) {
        this.materialRepository = materialRepository;
        this.stockRepository = stockRepository;
        this.access = access;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MaterialResponse> search(String category, Boolean active, PageQuery pageQuery) {
        String search = pageQuery.getSearch();
        Specification<Material> spec = (root, query, cb) -> category == null ? null : cb.equal(root.get("category"), category);
        spec = spec.and((root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active))
                .and((root, query, cb) -> search == null ? null
                        : cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase(Locale.ROOT) + "%"));
        return PageResponse.from(materialRepository.findAll(spec, pageQuery.toPageable(SORTS, "name", Sort.Direction.ASC)),
                MaterialServiceImpl::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LookupOption> activeOptions() {
        return materialRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(m -> new LookupOption(String.valueOf(m.getId()), m.getName() + " (" + m.getUnit() + ")"))
                .toList();
    }

    @Override
    @Transactional
    public MaterialResponse create(MaterialRequestBody request) {
        if (materialRepository.existsByNameIgnoreCase(request.getName())) {
            throw DuplicateRecordException.forField("name", "material.name.duplicate");
        }
        Material material = new Material(request.getName());
        apply(material, request);
        materialRepository.save(material);
        publish(material);
        return toResponse(material);
    }

    @Override
    @Transactional
    public MaterialResponse update(Long id, MaterialRequestBody request) {
        Material material = find(id);
        if (materialRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw DuplicateRecordException.forField("name", "material.name.duplicate");
        }
        apply(material, request);
        publish(material);
        return toResponse(material);
    }

    @Override
    @Transactional
    public MaterialResponse setActive(Long id, boolean active) {
        Material material = find(id);
        if (active) {
            material.activate();
        } else {
            material.deactivate();
        }
        publish(material);
        return toResponse(material);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponse> stock(Long projectId) {
        access.requireVisible(projectId);
        return stockRepository.findForProject(projectId).stream()
                .map(s -> new StockResponse(s.getMaterial().getId(), s.getMaterial().getName(),
                        s.getMaterial().getCategory(), s.getMaterial().getUnit(), s.getQuantity(), s.getUpdatedAt()))
                .toList();
    }

    private void apply(Material material, MaterialRequestBody r) {
        CompanySettings settings = SystemSettingsManager.getInstance().getSettings();
        if (!settings.materialCategories().contains(r.getCategory())) {
            throw BusinessRuleException.forField("category", "materialCategories.unknown");
        }
        if (!settings.materialUnits().contains(r.getUnit())) {
            throw BusinessRuleException.forField("unit", "material.unit.unknown");
        }
        material.update(r.getName(), r.getCategory(), r.getUnit(), r.getUnitPrice(), r.getDescription());
    }

    private void publish(Material material) {
        eventPublisher.publish(WorkflowEvent.byCurrentUser(WorkflowEventType.MATERIAL_SAVED)
                .entityId(material.getId()).args(material.getName()).build());
    }

    private Material find(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("resource.notFound", "Material"));
    }

    static MaterialResponse toResponse(Material m) {
        return new MaterialResponse(m.getId(), m.getName(), m.getCategory(), m.getUnit(), m.getUnitPrice(),
                m.getDescription(), m.isActive());
    }
}

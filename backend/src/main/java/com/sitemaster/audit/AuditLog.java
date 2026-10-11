package com.sitemaster.audit;

import com.sitemaster.common.entity.BaseEntity;
import com.sitemaster.common.enums.AppModule;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * One line of the audit trail. Written only by AuditLogObserver and never changed.
 * The actor's name is copied so the entry stays readable if the user is renamed.
 * <p>
 * OOP concepts: Inheritance (extends BaseEntity) and Encapsulation (immutable).
 */
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "actor_id", updatable = false)
    private Long actorId;

    @Column(name = "actor_name", nullable = false, length = 100, updatable = false)
    private String actorName;

    @Column(name = "action", nullable = false, length = 60, updatable = false)
    private String action;

    @Enumerated(EnumType.STRING)
    @Column(name = "module", nullable = false, length = 40, updatable = false)
    private AppModule module;

    @Column(name = "entity_type", length = 60, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(name = "description", nullable = false, length = 1000, updatable = false)
    private String description;

    protected AuditLog() {
        // required by JPA
    }

    public AuditLog(Long actorId, String actorName, String action, AppModule module,
                    String entityType, Long entityId, String description) {
        this.actorId = actorId;
        this.actorName = actorName;
        this.action = action;
        this.module = module;
        this.entityType = entityType;
        this.entityId = entityId;
        this.description = description;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public String getAction() {
        return action;
    }

    public AppModule getModule() {
        return module;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public String getDescription() {
        return description;
    }
}

package com.sitemaster.workflow;

import com.sitemaster.security.SecurityUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Something that happened in a workflow, published to every WorkflowObserver.
 * Built with {@link #builder(WorkflowEventType)}.
 * <p>
 * OOP concept: Encapsulation - immutable once built; collections are copied.
 *
 * @param type         what happened
 * @param actorId      user who did it (null for the system)
 * @param actorName    display name of the actor, kept in the audit log even if the user changes
 * @param entityId     id of the affected record
 * @param args         message arguments {1}, {2}... ({0} is the actor name)
 * @param recipientIds users who should receive an in-app notification
 * @param link         role-relative page to open from the notification, e.g. "profile"
 */
public record WorkflowEvent(WorkflowEventType type,
                            Long actorId,
                            String actorName,
                            Long entityId,
                            List<String> args,
                            Set<Long> recipientIds,
                            String link) {

    public WorkflowEvent {
        Objects.requireNonNull(type, "type");
        args = List.copyOf(args);
        recipientIds = Set.copyOf(recipientIds);
    }

    /** {actorName, arg1, arg2, ...} for message formatting. */
    public Object[] messageArguments() {
        List<Object> all = new ArrayList<>(args.size() + 1);
        all.add(actorName);
        all.addAll(args);
        return all.toArray();
    }

    public static Builder builder(WorkflowEventType type) {
        return new Builder(type);
    }

    /** Builder with the signed-in user (or "System" when nobody is signed in) as the actor. */
    public static Builder byCurrentUser(WorkflowEventType type) {
        Builder builder = new Builder(type);
        SecurityUtils.currentPrincipal().ifPresent(p -> builder.actor(p.getUserId(), p.getFullName()));
        return builder;
    }

    /** Fluent builder for WorkflowEvent. */
    public static final class Builder {

        private static final String SYSTEM_ACTOR = "System";

        private final WorkflowEventType type;
        private Long actorId;
        private String actorName = SYSTEM_ACTOR;
        private Long entityId;
        private final List<String> args = new ArrayList<>();
        private final Set<Long> recipientIds = new LinkedHashSet<>();
        private String link;

        private Builder(WorkflowEventType type) {
            this.type = type;
        }

        public Builder actor(Long id, String name) {
            this.actorId = id;
            this.actorName = name == null ? SYSTEM_ACTOR : name;
            return this;
        }

        public Builder entityId(Long id) {
            this.entityId = id;
            return this;
        }

        public Builder args(Object... values) {
            for (Object value : values) {
                this.args.add(value == null ? "" : String.valueOf(value));
            }
            return this;
        }

        public Builder recipients(Long... userIds) {
            for (Long id : userIds) {
                if (id != null) {
                    recipientIds.add(id);
                }
            }
            return this;
        }

        public Builder recipients(Iterable<Long> userIds) {
            userIds.forEach(id -> {
                if (id != null) {
                    recipientIds.add(id);
                }
            });
            return this;
        }

        public Builder link(String link) {
            this.link = link;
            return this;
        }

        public WorkflowEvent build() {
            return new WorkflowEvent(type, actorId, actorName, entityId, args, recipientIds, link);
        }
    }
}

package org.solocode.automotion.Machine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MachineBehaviorRegistry {

    private final MachineBehavior defaultBehavior;
    private final Map<String, MachineBehavior> behaviors = new HashMap<>();

    public MachineBehaviorRegistry(MachineBehavior defaultBehavior) {
        if (defaultBehavior == null) {
            throw new IllegalArgumentException("A default behavior is required");
        }
        this.defaultBehavior = defaultBehavior;
    }

    public void register(String machineId, MachineBehavior behavior) {
        if (behavior == null) {
            throw new IllegalArgumentException("Behavior must not be null for machine: " + machineId);
        }
        if (behaviors.putIfAbsent(machineId, behavior) != null) {
            throw new IllegalArgumentException(
                    "Behavior already registered for machine: " + machineId
            );
        }
    }

    public Optional<MachineBehavior> get(String machineId) {
        return Optional.ofNullable(behaviors.get(machineId));
    }

    /**
     * Returns the special behavior of a machine, or the shared default processor.
     */
    public MachineBehavior behaviorFor(String machineId) {
        return behaviors.getOrDefault(machineId, defaultBehavior);
    }

    public MachineBehavior defaultBehavior() {
        return defaultBehavior;
    }
}

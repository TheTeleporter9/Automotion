package org.solocode.automotion.Machine;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MachineBehaviorRegistry {
    private final Map<String, MachineBehavior> behaviors = new HashMap<>();

    public void register(String machineId, MachineBehavior behavior) {
        if (behaviors.putIfAbsent(machineId, behavior) != null) {
            throw new IllegalArgumentException(
                    "Behavior already registered for machine: " + machineId
            );
        }
    }

    public Optional<MachineBehavior> get(String machineId) {
        return Optional.ofNullable(behaviors.get(machineId));
    }
}

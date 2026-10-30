package org.solocode.automotion.Machine;

import org.solocode.automotion.Data.Machine;
import org.solocode.automotion.Data.MachineContext;

public interface MachineBehavior {

    /**
     * Tick cadence of this behavior. Special behaviors may override the default
     * interval configured on the machine definition.
     */
    default int interval(Machine machine) {
        return machine.properties().intervalTicks();
    }

    void update(MachineContext context);
}

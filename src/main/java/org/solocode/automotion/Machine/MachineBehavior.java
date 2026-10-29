package org.solocode.automotion.Machine;

import org.solocode.automotion.Data.MachineContext;

public interface MachineBehavior {
    default int interval() {
        return 20;
    }

    void update(MachineContext context);
}

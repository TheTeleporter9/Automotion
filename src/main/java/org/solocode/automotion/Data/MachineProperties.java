package org.solocode.automotion.Data;

import java.util.Objects;

public record MachineProperties(
        int durationTicks,
        int intervalTicks,
        MachineLayout layout
) {

    public static final int DEFAULT_DURATION_TICKS = 200;
    public static final int DEFAULT_INTERVAL_TICKS = 10;

    public MachineProperties {
        if (durationTicks < 1) {
            throw new IllegalArgumentException("DURATION must be at least 1 tick, got " + durationTicks);
        }
        if (intervalTicks < 1) {
            throw new IllegalArgumentException("INTERVAL must be at least 1 tick, got " + intervalTicks);
        }
        Objects.requireNonNull(layout, "layout");
    }

    public static MachineProperties defaults(String machineId) {
        return new MachineProperties(
                DEFAULT_DURATION_TICKS,
                DEFAULT_INTERVAL_TICKS,
                MachineLayout.defaults(machineId)
        );
    }

    public int progressPercent(int progressTicks) {
        long percent = progressTicks * 100L / durationTicks;
        return (int) Math.max(0, Math.min(100, percent));
    }
}

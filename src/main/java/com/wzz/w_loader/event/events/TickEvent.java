package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

public abstract class TickEvent extends Event {
    private final Phase phase;

    protected TickEvent(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return phase;
    }

    public enum Phase {
        START,
        END
    }
}

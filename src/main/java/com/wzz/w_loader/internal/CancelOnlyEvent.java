package com.wzz.w_loader.internal;

import com.wzz.w_loader.event.Event;

public final class CancelOnlyEvent extends Event {
    public CancelOnlyEvent() { setCancelled(true); }

    @Override
    public boolean isCancellable() { return true; }
}
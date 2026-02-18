package com.wzz.w_loader.internal;

import com.wzz.w_loader.event.Event;

final class CancelOnlyEvent extends Event {
    CancelOnlyEvent() { setCancelled(true); }

    @Override
    public boolean isCancellable() { return true; }
}
package com.wzz.w_loader.event;

public abstract class Event {
    private boolean cancelled = false;

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    /** 子类覆盖返回 false 表示该事件不可取消 */
    public boolean isCancellable() { return false; }
}
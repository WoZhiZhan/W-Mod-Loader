package com.wzz.w_loader.event;

public abstract class Event {
    private Result result = Result.DEFAULT;
    private boolean cancelled = false;

    public boolean isCancelled() { return cancelled; }

    public void setCancelled(boolean cancelled) {
        if (!this.isCancellable()) {
            throw new UnsupportedOperationException(
                    "Attempted to call Event#setCanceled() on a non-cancelable event of type: "
                            + this.getClass().getCanonicalName()
            );
        }
        this.cancelled = cancelled;
    }

    /** 子类覆盖返回 false 表示该事件不可取消 */
    public boolean isCancellable() { return false; }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public enum Result {
        DENY,
        DEFAULT,
        ALLOW
    }
}
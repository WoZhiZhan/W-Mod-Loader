package com.wzz.w_loader.hook;

@FunctionalInterface
public interface HookCallback {
    void call(HookContext ctx) throws Exception;
}
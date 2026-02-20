package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

public class ScreenInitEvent extends Event {
    private final Object screen;
    public ScreenInitEvent(Object screen) { this.screen = screen; }

    public net.minecraft.client.gui.screens.Screen getScreen() {
        return (net.minecraft.client.gui.screens.Screen) screen;
    }
}
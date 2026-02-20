package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

public class ScreenCloseEvent extends Event {
    private final Object currentScreen;

    public ScreenCloseEvent(Object currentScreen) {
        this.currentScreen = currentScreen;
    }

    public net.minecraft.client.gui.screens.Screen getScreen() {
        return (net.minecraft.client.gui.screens.Screen) currentScreen;
    }
}
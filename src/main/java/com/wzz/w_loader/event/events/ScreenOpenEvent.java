package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

public class ScreenOpenEvent extends Event {
    private final Object currentScreen;
    private Object newScreen;

    public ScreenOpenEvent(Object currentScreen, Object newScreen) {
        this.currentScreen = currentScreen;
        this.newScreen = newScreen;
    }

    @Override public boolean isCancellable() { return true; }

    public net.minecraft.client.gui.screens.Screen getCurrentScreen() {
        return (net.minecraft.client.gui.screens.Screen) currentScreen;
    }
    public net.minecraft.client.gui.screens.Screen getNewScreen() {
        return (net.minecraft.client.gui.screens.Screen) newScreen;
    }
    public void setNewScreen(net.minecraft.client.gui.screens.Screen screen) {
        this.newScreen = screen;
    }
}
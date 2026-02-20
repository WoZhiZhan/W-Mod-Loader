package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;

public class ScreenRenderEvent extends Event {
    private final Object screen;
    private final Object graphics;
    private final int mouseX, mouseY;
    private final float partialTick;

    public ScreenRenderEvent(Object screen, Object graphics, int mouseX, int mouseY, float partialTick) {
        this.screen = screen; this.graphics = graphics;
        this.mouseX = mouseX; this.mouseY = mouseY; this.partialTick = partialTick;
    }

    @Override public boolean isCancellable() { return true; }

    public net.minecraft.client.gui.screens.Screen getScreen() {
        return (net.minecraft.client.gui.screens.Screen) screen;
    }
    public net.minecraft.client.gui.GuiGraphics getGraphics() {
        return (net.minecraft.client.gui.GuiGraphics) graphics;
    }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }
    public float getPartialTick() { return partialTick; }
}
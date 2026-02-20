package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;

public class RenderTooltipBackgroundEvent extends Event {
    private final GuiGraphics guiGraphics;
    private final int x, y, width, height;
    private final Identifier resource;

    public RenderTooltipBackgroundEvent(GuiGraphics guiGraphics, int x, int y, int width, int height, Identifier resource) {
        this.guiGraphics = guiGraphics;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.resource = resource;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public Identifier getResource() {
        return resource;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}

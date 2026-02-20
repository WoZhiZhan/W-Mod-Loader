package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RenderTooltipEvent extends Event {
    private final GuiGraphics guiGraphics;
    private final ItemStack itemStack;
    private List<ClientTooltipComponent> tooltipLines;
    private final int mouseX;
    private final int mouseY;
    private Font font;
    private boolean modified = false;

    public RenderTooltipEvent(GuiGraphics guiGraphics, ItemStack itemStack, List<ClientTooltipComponent> tooltipLines,
                              int mouseX, int mouseY, Font font) {
        this.guiGraphics = guiGraphics;
        this.itemStack = itemStack;
        this.tooltipLines = new ArrayList<>(tooltipLines);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.font = font;
    }

    public GuiGraphics getGuiGraphics() {
        return guiGraphics;
    }

    public ItemStack getItemStack() { return itemStack; }
    public List<ClientTooltipComponent> getTooltipLines() { return tooltipLines; }
    public int getMouseX() { return mouseX; }
    public int getMouseY() { return mouseY; }

    public Font getFont() {
        return font;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public boolean isModified() { return modified; }

    public void setTooltipLines(List<ClientTooltipComponent> tooltipLines) {
        this.tooltipLines = tooltipLines;
        this.modified = true;
    }

    public void setFont(Font font) {
        this.font = font;
        this.modified = true;
    }

    public void addLine(String text) {
        List<ClientTooltipComponent> newLines = new ArrayList<>(tooltipLines);
        newLines.add(ClientTooltipComponent.create(Component.literal(text).getVisualOrderText()));
        this.tooltipLines = newLines;
        this.modified = true;
    }

    public void addLine(Component component) {
        List<ClientTooltipComponent> newLines = new ArrayList<>(tooltipLines);
        newLines.add(ClientTooltipComponent.create(component.getVisualOrderText()));
        this.tooltipLines = newLines;
        this.modified = true;
    }
}
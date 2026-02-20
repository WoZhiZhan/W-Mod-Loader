package com.wzz.w_loader.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;

public class WModUtils {
    public static ItemStack getHoveredItem(int mouseX, int mouseY) {
        try {
            Minecraft mc = Minecraft.getInstance();
            Screen currentScreen = mc.screen;
            if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
                Field hoveredSlotField = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
                hoveredSlotField.setAccessible(true);
                Slot hoveredSlot = (Slot) hoveredSlotField.get(containerScreen);
                if (hoveredSlot != null && hoveredSlot.hasItem()) {
                    return hoveredSlot.getItem();
                }
                int leftPos = containerScreen.getRectangle().left();
                int topPos = containerScreen.getRectangle().top();
                double relX = mouseX - leftPos;
                double relY = mouseY - topPos;
                for (Slot slot : containerScreen.getMenu().slots) {
                    if (slot.isActive() && isHovering(slot, relX, relY)) {
                        if (slot.hasItem()) {
                            return slot.getItem();
                        }
                    }
                }
            }
            return ItemStack.EMPTY;
        } catch (Exception e) {
            return ItemStack.EMPTY;
        }
    }

    private static boolean isHovering(Slot slot, double x, double y) {
        return x >= slot.x - 1 && x < slot.x + 17 &&
                y >= slot.y - 1 && y < slot.y + 17;
    }
}

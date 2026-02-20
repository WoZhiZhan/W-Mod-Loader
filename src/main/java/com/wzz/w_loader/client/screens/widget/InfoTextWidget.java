package com.wzz.w_loader.client.screens.widget;

import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.bootstrap.Bootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;

public class InfoTextWidget extends AbstractWidget {
    private final TitleScreen screen;

    public InfoTextWidget(TitleScreen screen) {
        super(2, screen.height - 20, 200, 10, Component.empty());
        this.screen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int modCount = ModLoader.INSTANCE.getLoadedMods().size();
        String info = String.format(
                Component.translatable("w_loader.mods.loader_info").getString(),
                Bootstrap.version,
                modCount,
                modCount == 1 ? "" : "s"
        );

        graphics.drawString(screen.getFont(),
                Component.literal(info),
                this.getX(), this.getY(),
                0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
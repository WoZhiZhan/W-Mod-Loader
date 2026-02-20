package com.wzz.w_loader.client.screens;

import com.wzz.w_loader.LoadedMod;
import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.ModMetadata;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;

public class ModListScreen extends Screen {

    private static final int LIST_WIDTH = 160;
    private static final int PADDING    = 6;

    private final Screen parent;
    private ModListWidget modListWidget;
    private ModEntry selected;

    public ModListScreen(Screen parent) {
        super(Component.literal("Mods"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        modListWidget = new ModListWidget(
                this.minecraft,
                LIST_WIDTH,
                this.height - 32 - PADDING,
                32,
                PADDING
        );
        this.addRenderableWidget(modListWidget);

        int doneW = Math.min(this.width - LIST_WIDTH - PADDING * 3, 200);
        int doneX = LIST_WIDTH + PADDING * 2 + (this.width - LIST_WIDTH - PADDING * 3 - doneW) / 2;
        this.addRenderableWidget(
                Button.builder(
                                Component.translatable("gui.done"),
                                btn -> this.minecraft.setScreen(parent)
                        )
                        .bounds(doneX, this.height - 26, doneW, 20)
                        .build()
        );
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.isInGameUi()) {
            this.renderTransparentBackground(graphics);
        } else {
            if (this.minecraft.level == null) {
                this.renderPanorama(graphics, partialTick);
            }
            this.renderMenuBackground(graphics);
        }
        this.minecraft.gui.renderDeferredSubtitles();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        // 标题
        graphics.drawCenteredString(this.font,
                Component.translatable("w_loader.mods.installed"),
                LIST_WIDTH / 2 + PADDING, 12, 0xFFFFFFFF);

        // 右侧 info 面板
        int infoX = LIST_WIDTH + PADDING * 2;
        int infoW  = this.width - infoX - PADDING;

        if (selected != null) {
            ModMetadata meta = selected.mod.metadata();
            int y = 36;

            // 模组名称
            graphics.drawString(this.font,
                    Component.literal(meta.name()).withStyle(style -> style.withBold(true)),
                    infoX, y, 0xFFFFFFFF);
            y += 16;

            // 版本和ID
            String versionId = Component.translatable("w_loader.mods.version").getString() + ": " + meta.version();
            if (meta.modId != null && !meta.modId.isEmpty()) {
                versionId += "  |  " + Component.translatable("w_loader.mods.id").getString() + ": " + meta.modId;
            }
            graphics.drawString(this.font,
                    Component.literal(versionId),
                    infoX, y, 0xFFCCCCCC);
            y += 20;

            // 分隔线
            graphics.fill(infoX, y, infoX + infoW, y + 1, 0x66FFFFFF);
            y += 10;

            // 介绍文字
            graphics.drawString(this.font,
                    Component.translatable("w_loader.mods.description").withStyle(style -> style.withBold(true)),
                    infoX, y, 0xFFFFFFFF);
            y += 12;

            if (meta.description != null && !meta.description.isEmpty()) {
                String desc = meta.description;
                List<FormattedText> lines = this.font.getSplitter()
                        .splitLines(desc, infoW - 10, Style.EMPTY);

                for (FormattedText line : lines) {
                    graphics.drawString(this.font,
                            line.getString(),
                            infoX, y, 0xFFAAAAAA);
                    y += 12;
                }
            } else {
                graphics.drawString(this.font,
                        Component.translatable("w_loader.mods.no_description").withStyle(style -> style.withItalic(true)),
                        infoX, y, 0xFF888888);
                y += 12;
            }
            y += 6;

            // 分隔线
            graphics.fill(infoX, y, infoX + infoW, y + 1, 0x66FFFFFF);
            y += 10;

            // 主类信息
            graphics.drawString(this.font,
                    Component.translatable("w_loader.mods.main_class").withStyle(style -> style.withBold(true)),
                    infoX, y, 0xFFFFFFFF);
            y += 12;

            String mainClass = meta.main();
            if (mainClass != null && !mainClass.isEmpty()) {
                if (this.font.width(mainClass) > infoW - 10) {
                    mainClass = this.font.plainSubstrByWidth(mainClass, infoW - 20) + "...";
                }
                graphics.drawString(this.font,
                        Component.literal(mainClass),
                        infoX + 10, y, 0xFFAAAAAA);
            }

        } else {
            // 没有选中任何模组
            graphics.drawCenteredString(this.font,
                    Component.translatable("w_loader.mods.select"),
                    infoX + infoW / 2,
                    this.height / 2,
                    0xFF888888);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    void setSelected(ModEntry entry) {
        this.selected = (entry == this.selected) ? null : entry;
    }

    class ModListWidget extends ObjectSelectionList<ModEntry> {

        ModListWidget(Minecraft mc, int width, int height, int y, int x) {
            super(mc, width, height, y, 26);
            this.setX(x);

            for (LoadedMod mod : ModLoader.INSTANCE.getLoadedMods()) {
                this.addEntry(new ModEntry(mod));
            }
        }

        @Override
        public int getRowWidth() {
            return this.getWidth() - 6;
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput output) {}
    }

    class ModEntry extends ObjectSelectionList.Entry<ModEntry> {

        final LoadedMod mod;

        ModEntry(LoadedMod mod) {
            this.mod = mod;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY,
                                  boolean hovered, float partialTick) {
            if (hovered || modListWidget.getSelected() == this) {
                graphics.fill(this.getX(), this.getY(),
                        this.getX() + this.getWidth(), this.getY() + this.getHeight(),
                        hovered ? 0x33FFFFFF : 0x554A90D9);
            }

            ModMetadata meta = mod.metadata();

            graphics.drawString(minecraft.font,
                    Component.literal(meta.name()),
                    this.getX() + 6, this.getY() + 4, 0xFFFFFFFF);

            graphics.drawString(minecraft.font,
                    Component.literal(meta.version() != null ? meta.version() : ""),
                    this.getX() + 6, this.getY() + 14, 0xFFAAAAAA);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            ModListScreen.this.setSelected(this);
            modListWidget.setSelected(this);
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.literal(mod.metadata().name());
        }
    }
}
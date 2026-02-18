package com.wzz.w_loader.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public final class Registries {
    public static final Registry<Item>           ITEMS  = Registry.items();
    public static final Registry<Block>          BLOCKS = Registry.blocks();
    public static final Registry<CreativeModeTab> TABS  = Registry.tabs();

    public static void freezeAll() {
        ITEMS.freeze((id, item) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, Identifier.parse(id), item));
        BLOCKS.freeze((id, block) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, Identifier.parse(id), block));
        TABS.freeze((id, tab) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.parse(id), tab));
    }
}
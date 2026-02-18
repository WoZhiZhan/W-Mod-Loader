package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

public class BlockBreakEvent extends Event {
    private final Object player;
    private final Object blockPos;
    private final Object blockState;

    public BlockBreakEvent(Object player, Object blockPos, Object blockState) {
        this.player = player;
        this.blockPos = blockPos;
        this.blockState = blockState;
    }

    @Override public boolean isCancellable() { return true; }
    public Player getPlayer() { return (Player) player; }
    public BlockPos getBlockPos() { return (BlockPos) blockPos; }
    public BlockState getBlockState() { return (BlockState) blockState; }
}
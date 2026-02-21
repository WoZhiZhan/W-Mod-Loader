package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SetBlockEntityEvent extends Event {
    private final Level level;
    private final BlockEntity blockEntity;

    public SetBlockEntityEvent(Level level, BlockEntity blockEntity) {
        this.level = level;
        this.blockEntity = blockEntity;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public Level getLevel() {
        return level;
    }

    public BlockEntity getBlockEntity() {
        return blockEntity;
    }
}

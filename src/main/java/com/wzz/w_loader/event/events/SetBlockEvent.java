package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SetBlockEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final BlockState blockState;
    private final int updateFlags;
    private final int updateLimit;

    public SetBlockEvent(Level level, BlockPos pos, BlockState blockState, int updateFlags, int updateLimit) {
        this.level = level;
        this.pos = pos;
        this.blockState = blockState;
        this.updateFlags = updateFlags;
        this.updateLimit = updateLimit;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public Level getLevel() {
        return level;
    }

    public BlockPos getPos() {
        return pos;
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public int getUpdateFlags() {
        return updateFlags;
    }

    public int getUpdateLimit() {
        return updateLimit;
    }
}

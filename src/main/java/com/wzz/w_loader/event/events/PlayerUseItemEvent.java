package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class PlayerUseItemEvent extends Event {
    private final ServerPlayer player;
    private final Level level;
    private final ItemStack stack;
    private final InteractionHand hand;

    public PlayerUseItemEvent(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand) {
        this.player = player;
        this.level = level;
        this.stack = stack;
        this.hand = hand;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Level getLevel() {
        return level;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}

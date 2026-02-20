package com.wzz.w_loader.event.events;

import com.google.common.base.Preconditions;
import com.wzz.w_loader.event.Event;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

public abstract class PlayerInteractEvent extends Event {
    private final Object player;
    private final Object hand;
    private final Object pos;
    private final Object face;
    private Object cancellationResult;

    protected PlayerInteractEvent(Object player, Object hand, Object pos, Object face) {
        this.player = player;
        this.cancellationResult = InteractionResult.PASS;
        this.hand = Preconditions.checkNotNull(hand, "Null hand in PlayerInteractEvent!");
        this.pos = Preconditions.checkNotNull(pos, "Null position in PlayerInteractEvent!");
        this.face = face;
    }

    public Player getPlayer() {
        return (Player) player;
    }

    public BlockPos getPos() {
        return (BlockPos) pos;
    }

    public Direction getFace() {
        return (Direction) face;
    }

    public InteractionHand getHand() {
        return (InteractionHand) hand;
    }

    public InteractionResult getCancellationResult() {
        return (InteractionResult) cancellationResult;
    }

    public void setCancellationResult(InteractionResult result) {
        this.cancellationResult = result;
    }
}

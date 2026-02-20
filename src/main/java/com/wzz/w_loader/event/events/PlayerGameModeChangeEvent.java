package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

public class PlayerGameModeChangeEvent extends Event {
    private final ServerPlayer player;
    private final GameType gameType;
    private GameType previousGameModeForPlayer;

    public PlayerGameModeChangeEvent(ServerPlayer player, GameType gameType, GameType previousGameModeForPlayer) {
        this.player = player;
        this.gameType = gameType;
        this.previousGameModeForPlayer = previousGameModeForPlayer;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }

    public void setGameType(GameType gameType) {
        this.previousGameModeForPlayer = gameType;
    }

    public GameType getGameType() {
        return gameType;
    }

    public GameType getPreviousGameModeForPlayer() {
        return previousGameModeForPlayer;
    }

    public ServerPlayer getPlayer() {
        return player;
    }
}

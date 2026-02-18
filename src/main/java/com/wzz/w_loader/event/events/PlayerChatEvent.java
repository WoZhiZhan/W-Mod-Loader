package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import com.wzz.w_loader.hook.MessageOverride;
import com.wzz.w_loader.util.ReflectUtil;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

public class PlayerChatEvent extends Event {
    private final Object player;
    private String message;

    public PlayerChatEvent(Object player, String message) {
        this.player = player;
        this.message = message;
    }

    @Override public boolean isCancellable() { return true; }
    public Player getPlayer() { return (Player) player; }
    public String getMessage() { return message; }
    public void setMessage(String message) {
        this.message = message;
        UUID uuid = ReflectUtil.invoke(
                ReflectUtil.invoke(player, "getGameProfile"), "id");
        MessageOverride.set(uuid, message);
    }
}
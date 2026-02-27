package com.wzz.w_loader.hook;

import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;

public class GameLifeCycleHook {
    static Minecraft minecraft = Minecraft.getInstance();
    static MinecraftServer server = null;
    public static MinecraftServer getServer(){
        return server;
    }
    public static Minecraft getMinecraft(){
        if (minecraft == null){
            minecraft = Minecraft.getInstance();
        }
        return minecraft;
    }
    public static boolean isOnClientMainThread() {
        Minecraft mc = getMinecraft();
        return mc != null && mc.isSameThread();
    }
    public static boolean isOnServerMainThread() {
        MinecraftServer server = getServer();
        return server != null && server.isSameThread();
    }
}

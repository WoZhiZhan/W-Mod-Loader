package com.wzz.w_loader.common;

import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.annotation.Subscribe;
import com.wzz.w_loader.annotation.WMod;
import com.wzz.w_loader.command.WCommandBuilder;
import com.wzz.w_loader.event.events.*;
import net.minecraft.network.chat.Component;

@WMod(name = "W Loader", modId = "w_loader")
public class WLoaderMod {

    @Subscribe
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                WCommandBuilder.create("loader")
                        .requires(2)
                        .literal("mod", modNode -> modNode
                                .executes(context -> {
                                    var loadedMods = ModLoader.INSTANCE.getLoadedMods();
                                    context.getSource().sendSystemMessage(Component.literal("§6[W-Loader]§f 已加载模组列表:"));
                                    if (loadedMods.isEmpty()) {
                                        context.getSource().sendSystemMessage(Component.literal("§7  (无外部模组)"));
                                    } else {
                                        loadedMods.forEach(pm -> {
                                            String msg = String.format("§a[●] §f%s §7(%s) §8- %s",
                                                    pm.metadata().name, pm.metadata().version, pm.metadata().modId);
                                            context.getSource().sendSystemMessage(Component.literal(msg));
                                        });
                                    }
                                    return 1;
                                })
                        )
                        .build()
        );
    }
}
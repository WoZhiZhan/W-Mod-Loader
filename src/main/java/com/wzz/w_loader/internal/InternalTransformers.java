package com.wzz.w_loader.internal;

import com.wzz.w_loader.event.EventBus;
import com.wzz.w_loader.event.events.*;
import com.wzz.w_loader.hook.HookManager;
import com.wzz.w_loader.hook.MessageOverride;
import com.wzz.w_loader.resource.ModRepositorySource;
import com.wzz.w_loader.transform.TransformerRegistry;
import com.wzz.w_loader.internal.transformer.UniversalTransformer;
import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.util.ReflectUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class InternalTransformers {

    private static final String[] HOOKED_CLASSES = {
            "net/minecraft/server/MinecraftServer",
            "net/minecraft/client/server/IntegratedServer",
            "net/minecraft/server/players/PlayerList",
            "net/minecraft/server/network/ServerGamePacketListenerImpl",
            "net/minecraft/server/level/ServerPlayerGameMode",
            "net/minecraft/core/registries/BuiltInRegistries",
            "net/minecraft/server/packs/repository/PackRepository",
            "net/minecraft/world/item/CreativeModeTab",
    };

    public static void registerAll() {
        TransformerRegistry registry = TransformerRegistry.getInstance();
        for (String cls : HOOKED_CLASSES) {
            registry.register(new UniversalTransformer(cls));
        }

        HookManager.on("net/minecraft/server/packs/repository/PackRepository")
                .method("reload")
                .atHead()
                .inject(ctx -> {
                    Object self = ctx.getSelf();
                    Set<Object> sources = ReflectUtil.getField(self, "sources");
                    boolean alreadyInjected = sources.stream()
                            .anyMatch(s -> s instanceof ModRepositorySource);
                    if (alreadyInjected) return;
                    Set<Object> newSources = new java.util.LinkedHashSet<>(sources);
                    newSources.add(new ModRepositorySource());
                    ReflectUtil.setField(self, "sources",
                            com.google.common.collect.ImmutableSet.copyOf(newSources));
                });

        HookManager.on("net/minecraft/world/item/CreativeModeTab")
                .method("buildContents")
                .descriptor("(Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;)V")
                .atTail()
                .inject(ctx -> {
                    Object tab = ctx.getSelf();
                    net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB
                            .getResourceKey((net.minecraft.world.item.CreativeModeTab) tab)
                            .ifPresent(key -> {
                                List<Object> toAdd = CreativeTabEventCache.get(key);
                                if (toAdd == null || toAdd.isEmpty()) return;

                                Collection<net.minecraft.world.item.ItemStack> displayItems =
                                        ReflectUtil.getField(tab, "displayItems");
                                Collection<net.minecraft.world.item.ItemStack> searchItems =
                                        ReflectUtil.getField(tab, "displayItemsSearchTab");

                                for (Object itemObj : toAdd) {
                                    net.minecraft.world.item.ItemStack stack =
                                            new net.minecraft.world.item.ItemStack(
                                                    (net.minecraft.world.level.ItemLike) itemObj);
                                    displayItems.add(stack);
                                    searchItems.add(stack);
                                }
                            });
                });

        HookManager.on("net/minecraft/core/registries/BuiltInRegistries")
                .method("createContents")
                .atTail()
                .inject(ctx -> ModLoader.onBootstrap());

        HookManager.on("net/minecraft/server/MinecraftServer")
                .method("runServer")
                .atHead()
                .inject(ctx -> ModLoader.onMinecraftInit());

        HookManager.on("net/minecraft/client/server/IntegratedServer")
                .method("tickServer")
                .descriptor("(Ljava/util/function/BooleanSupplier;)V")
                .atHead()
                .inject(ctx -> EventBus.INSTANCE.post(new ServerTickEvent(ctx.getSelf())));

        HookManager.on("net/minecraft/server/players/PlayerList")
                .method("placeNewPlayer")
                .atTail()
                .inject(ctx -> EventBus.INSTANCE.post(new PlayerJoinEvent(ctx.getArg(2))));

        HookManager.on("net/minecraft/server/network/ServerGamePacketListenerImpl")
                .method("handleChat")
                .descriptor("(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V")
                .atHead()
                .inject(ctx -> {
                    Object self    = ctx.getSelf();
                    Object player  = ReflectUtil.getField(self, "player");
                    String message = ReflectUtil.invoke(ctx.getArg(1), "message");

                    PlayerChatEvent event = new PlayerChatEvent(player, message);
                    ctx.post(event);
                });

        HookManager.on("net/minecraft/server/level/ServerPlayerGameMode")
                .method("destroyBlock")
                .descriptor("(Lnet/minecraft/core/BlockPos;)Z")
                .atHead()
                .inject(ctx -> {
                    Object player   = ReflectUtil.getField(ctx.getSelf(), "player");
                    Object blockPos = ctx.getArg(1);
                    ctx.post(new BlockBreakEvent(player, blockPos, null));
                });

        HookManager.on("net/minecraft/server/network/ServerGamePacketListenerImpl")
                .method("broadcastChatMessage")
                .descriptor("(Lnet/minecraft/network/chat/PlayerChatMessage;)V")
                .atHead()
                .inject(ctx -> {
                    Object self   = ctx.getSelf();
                    Object player = ReflectUtil.getField(self, "player");
                    UUID uuid = ReflectUtil.invoke(
                            ReflectUtil.invoke(player, "getGameProfile"), "id");
                    String override = MessageOverride.getAndClear(uuid);
                    if (override == null) return;

                    Object original  = ctx.getArg(1);
                    Object component = ReflectUtil.invokeStatic(
                            "net.minecraft.network.chat.Component", "literal", override);
                    Object replaced  = ReflectUtil.invoke(original, "withUnsignedContent", component);
                    ReflectUtil.invoke(self, "broadcastChatMessage", replaced);
                    ctx.post(new CancelOnlyEvent());
                });
    }

    private InternalTransformers() {}
}
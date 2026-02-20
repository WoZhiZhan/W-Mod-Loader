package com.wzz.w_loader.hook;

import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.client.screens.widget.InfoTextWidget;
import com.wzz.w_loader.event.EventBus;
import com.wzz.w_loader.event.events.*;
import com.wzz.w_loader.internal.CancelOnlyEvent;
import com.wzz.w_loader.resource.ModRepositorySource;
import com.wzz.w_loader.util.ReflectUtil;
import com.wzz.w_loader.util.WModUtils;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VanillaHooks {

    @Hook(
            cls = "net/minecraft/server/MinecraftServer",
            method = "runServer"
    )
    public static void onServerStart(HookContext ctx) {
        ModLoader.onMinecraftInit();
        EventBus.INSTANCE.post(new ServerStartingEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/client/server/IntegratedServer",
            method = "tickServer",
            descriptor = "(Ljava/util/function/BooleanSupplier;)V"
    )
    public static void onServerTick(HookContext ctx) {
        EventBus.INSTANCE.post(new ServerTickEvent(ctx.getSelf(), TickEvent.Phase.START));
    }

    @Hook(
            cls = "net/minecraft/server/players/PlayerList",
            method = "placeNewPlayer",
            at = HookPoint.Position.TAIL
    )
    public static void onPlayerJoin(HookContext ctx) {
        EventBus.INSTANCE.post(new PlayerJoinEvent(ctx.getArg(2)));
    }

    @Hook(
            cls = "net/minecraft/server/network/ServerGamePacketListenerImpl",
            method = "handleChat",
            descriptor = "(Lnet/minecraft/network/protocol/game/ServerboundChatPacket;)V"
    )
    public static void onHandleChat(HookContext ctx) {
        Object player  = ReflectUtil.getField(ctx.getSelf(), "player");
        String message = ReflectUtil.invoke(ctx.getArg(1), "message");
        ctx.post(new PlayerChatEvent(player, message));
    }

    @Hook(
            cls = "net/minecraft/server/network/ServerGamePacketListenerImpl",
            method = "broadcastChatMessage",
            descriptor = "(Lnet/minecraft/network/chat/PlayerChatMessage;)V"
    )
    public static void onBroadcastChat(HookContext ctx) {
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
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerPlayerGameMode",
            method = "destroyBlock",
            descriptor = "(Lnet/minecraft/core/BlockPos;)Z"
    )
    public static void onBlockBreak(HookContext ctx) {
        Object player   = ReflectUtil.getField(ctx.getSelf(), "player");
        Object blockPos = ctx.getArg(1);
        ctx.post(new BlockBreakEvent(player, blockPos, null));
    }

    @Hook(
            cls = "net/minecraft/server/packs/repository/PackRepository",
            method = "reload"
    )
    public static void onPackRepositoryReload(HookContext ctx) {
        Object self = ctx.getSelf();
        Set<Object> sources = ReflectUtil.getField(self, "sources");
        boolean alreadyInjected = sources.stream()
                .anyMatch(s -> s instanceof ModRepositorySource);
        if (alreadyInjected) return;
        Set<Object> newSources = new java.util.LinkedHashSet<>(sources);
        newSources.add(new ModRepositorySource());
        ReflectUtil.setField(self, "sources",
                com.google.common.collect.ImmutableSet.copyOf(newSources));
    }

    @Hook(
            cls = "net/minecraft/world/item/CreativeModeTab",
            method = "buildContents",
            descriptor = "(Lnet/minecraft/world/item/CreativeModeTab$ItemDisplayParameters;)V",
            at = HookPoint.Position.TAIL
    )
    public static void onBuildCreativeTab(HookContext ctx) {
        Object tab = ctx.getSelf();
        net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB
                .getResourceKey((net.minecraft.world.item.CreativeModeTab) tab)
                .ifPresent(key -> {
                    List<Object> toAdd = CreativeTabEventCache.get(key);
                    if (toAdd == null || toAdd.isEmpty()) return;

                    Collection<ItemStack> displayItems =
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
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "hurtServer",
            descriptor = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
    )
    public static void onLivingAttack(HookContext ctx) {
        ctx.post(new LivingAttackEvent(ctx.getSelf(), ctx.getArg(2), ctx.getArg(3)));
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "actuallyHurt",
            descriptor = "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"
    )
    public static void onLivingHurt(HookContext ctx) {
        float damage = ctx.getArg(3);
        LivingHurtEvent event = new LivingHurtEvent(ctx.getSelf(), ctx.getArg(2), damage);
        ctx.post(event);
        if (event.getDamage() != damage) {
            ctx.setArg(3, event.getDamage()); // transformer 自动写回 slot
        }
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerPlayer",
            method = "die",
            descriptor = "(Lnet/minecraft/world/damagesource/DamageSource;)V"
    )
    public static void onPlayerDeath(HookContext ctx) {
        ctx.post(new LivingDeathEvent(ctx.getSelf(), ctx.getArg(1)));
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "die",
            descriptor = "(Lnet/minecraft/world/damagesource/DamageSource;)V"
    )
    public static void onLivingDie(HookContext ctx) {
        Object entity = ctx.getSelf();
        Object source = ctx.getArg(1);
        ctx.post(new LivingDeathEvent(entity, source));
    }

    @Hook(
            cls = "net/minecraft/world/entity/Entity",
            method = "kill",
            descriptor = "(Lnet/minecraft/server/level/ServerLevel;)V"
    )
    public static void onEntityKill(HookContext ctx) {
        ctx.post(new EntityKillEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/client/gui/screens/Screen",
            method = "init",
            descriptor = "(II)V",
            at = HookPoint.Position.TAIL
    )
    public static void onScreenInit(HookContext ctx) {
        EventBus.INSTANCE.post(new ScreenInitEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/client/gui/screens/Screen",
            method = "render",
            descriptor = "(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"
    )
    public static void onScreenRender(HookContext ctx) {
        ctx.post(new ScreenRenderEvent(
                ctx.getSelf(),
                ctx.getArg(1),
                ctx.getArg(2),
                ctx.getArg(3),
                ctx.getArg(4)
        ));
    }

    @Hook(
            cls = "net/minecraft/client/Minecraft",
            method = "setScreen",
            descriptor = "(Lnet/minecraft/client/gui/screens/Screen;)V"
    )
    public static void onScreenOpen(HookContext ctx) {
        Object current = ReflectUtil.getField(ctx.getSelf(), "screen");
        ScreenOpenEvent event = new ScreenOpenEvent(current, ctx.getArg(1));
        ctx.post(event);
        if (event.getNewScreen() != ctx.getArg(1)) {
            ctx.setArg(1, event.getNewScreen());
        }
    }

    @Hook(
            cls = "net/minecraft/client/gui/screens/TitleScreen",
            method = "init",
            descriptor = "()V",
            at = HookPoint.Position.TAIL
    )
    public static void onTitleScreenInit(HookContext ctx) {
        TitleScreen screen = ctx.getSelf();
        int cx = screen.width / 2;
        int realmsY = screen.height / 4 + 48 + 48;
        for (net.minecraft.client.gui.components.events.GuiEventListener child : screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget) {
                if (widget.getY() == realmsY && widget.getWidth() == 200) {
                    widget.setX(cx + 2);
                    widget.setWidth(98);
                    break;
                }
            }
        }
        net.minecraft.client.gui.components.Button modsButton =
                net.minecraft.client.gui.components.Button.builder(
                                net.minecraft.network.chat.Component.literal("Mods"),
                                btn -> net.minecraft.client.Minecraft.getInstance()
                                        .setScreen(new com.wzz.w_loader.client.screens.ModListScreen(screen))
                        )
                        .bounds(cx - 100, realmsY, 98, 20)
                        .build();
        ReflectUtil.invoke(screen, "addRenderableWidget", modsButton);
        ReflectUtil.invoke(screen, "addRenderableWidget", new InfoTextWidget(screen));
    }

    @Hook(
            cls = "net/minecraft/client/gui/screens/Screen",
            method = "onClose",
            descriptor = "()V"
    )
    public static void onScreenClose(HookContext ctx) {
        EventBus.INSTANCE.post(new ScreenCloseEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/client/server/IntegratedServer",
            method = "tickServer",
            descriptor = "(Ljava/util/function/BooleanSupplier;)V",
            at = HookPoint.Position.TAIL
    )
    public static void onServerTickEnd(HookContext ctx) {
        EventBus.INSTANCE.post(new ServerTickEvent(ctx.getSelf(), TickEvent.Phase.END));
    }

    @Hook(
            cls = "net/minecraft/client/Minecraft",
            method = "tick",
            descriptor = "()V"
    )
    public static void onClientTick(HookContext ctx) {
        EventBus.INSTANCE.post(new ClientTickEvent(TickEvent.Phase.START));
    }

    @Hook(
            cls = "net/minecraft/client/Minecraft",
            method = "tick",
            descriptor = "()V",
            at = HookPoint.Position.TAIL
    )
    public static void onClientTickEnd(HookContext ctx) {
        EventBus.INSTANCE.post(new ClientTickEvent(TickEvent.Phase.END));
    }

    @Hook(
            cls = "net/minecraft/client/renderer/GameRenderer",
            method = "tick",
            descriptor = "()V"
    )
    public static void onRenderTick(HookContext ctx) {
        EventBus.INSTANCE.post(new RenderTickEvent(TickEvent.Phase.START));
    }

    @Hook(
            cls = "net/minecraft/client/renderer/GameRenderer",
            method = "tick",
            descriptor = "()V",
            at = HookPoint.Position.TAIL
    )
    public static void onRenderTickEnd(HookContext ctx) {
        EventBus.INSTANCE.post(new RenderTickEvent(TickEvent.Phase.END));
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerLevel",
            method = "addEntity",
            descriptor = "(Lnet/minecraft/world/entity/Entity;)Z"
    )
    public static void onEntityJoinLevel(HookContext ctx) {
        ctx.post(new EntityJoinLevelEvent(ctx.getSelf(), ctx.getArg(1)));
    }

    @Hook(
            cls = "net/minecraft/client/renderer/entity/EntityRenderers",
            method = "<clinit>",
            at = HookPoint.Position.TAIL
    )
    public static void onEntityRenderersInit(HookContext ctx) {
        EventBus.INSTANCE.post(new EntityRegisterRenderersEvent());
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerPlayerGameMode",
            method = "useItemOn",
            descriptor = "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
            at = HookPoint.Position.HEAD
    )
    public static void onRightClickBlockServer(HookContext ctx) {
        ServerPlayer player = ctx.getArg(1);
        InteractionHand hand = ctx.getArg(4);
        BlockHitResult hitResult = ctx.getArg(5);
        PlayerRightClickBlockEvent event = new PlayerRightClickBlockEvent(
                player, hand, hitResult.getBlockPos(), hitResult
        );
        ctx.post(event);
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerLevel",
            method = "<init>",
            descriptor = "(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/dimension/LevelStem;ZJLjava/util/List;Z)V",
            at = HookPoint.Position.TAIL
    )
    public static void onServerLevelLoad(HookContext ctx) {
        ServerLevel level = ctx.getSelf();
        LevelEvent.Load event = new LevelEvent.Load(level);
        EventBus.INSTANCE.post(event);
    }

    @Hook(
            cls = "net/minecraft/client/multiplayer/ClientLevel",
            method = "<init>",
            descriptor = "(Lnet/minecraft/client/multiplayer/ClientPacketListener;Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/Holder;IILnet/minecraft/client/renderer/LevelRenderer;ZJI)V",
            at = HookPoint.Position.TAIL
    )
    public static void onClientLevelLoad(HookContext ctx) {
        ClientLevel level = ctx.getSelf();
        LevelEvent.Load event = new LevelEvent.Load(level);
        EventBus.INSTANCE.post(event);
    }

    @Hook(
            cls = "net/minecraft/world/level/Level",
            method = "close",
            descriptor = "()V",
            at = HookPoint.Position.HEAD
    )
    public static void onLevelClose(HookContext ctx) {
        Level level = ctx.getSelf();
        LevelEvent.Unload event = new LevelEvent.Unload(level);
        EventBus.INSTANCE.post(event);
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "heal",
            descriptor = "(F)V"
    )
    public static void onLivingHeal(HookContext ctx) {
        float value = ctx.getArg(1);
        LivingHealEvent event = new LivingHealEvent(ctx.getSelf(), value);
        ctx.post(event);
        if (event.getValue() != value) {
            ctx.setArg(1, event.getValue());
        }
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "setHealth",
            descriptor = "(F)V"
    )
    public static void onLivingSetHealth(HookContext ctx) {
        float value = ctx.getArg(1);
        LivingSetHealthEvent event = new LivingSetHealthEvent(ctx.getSelf(), value);
        ctx.post(event);
        if (event.getValue() != value) {
            ctx.setArg(1, event.getValue());
        }
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "swing",
            descriptor = "(Lnet/minecraft/world/InteractionHand;Z)V"
    )
    public static void onLivingSwing(HookContext ctx) {
        boolean sendToSwingingEntity = ctx.getArg(2);
        LivingSwingEvent event = new LivingSwingEvent(ctx.getSelf(), sendToSwingingEntity);
        ctx.post(event);
        if (event.isSendToSwingingEntity() != sendToSwingingEntity) {
            ctx.setArg(2, event.isSendToSwingingEntity());
        }
    }

    @Hook(
            cls = "net/minecraft/world/entity/Entity",
            method = "discard",
            descriptor = "()V"
    )
    public static void onEntityDiscard(HookContext ctx) {
        ctx.post(new EntityDiscardEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/world/entity/Entity",
            method = "remove",
            descriptor = "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"
    )
    public static void onEntityRemove(HookContext ctx) {
        Entity.RemovalReason removalReason = ctx.getArg(1);
        EntityRemoveEvent removeEvent = new EntityRemoveEvent(ctx.getSelf(), removalReason);
        ctx.post(removeEvent);
        if (removeEvent.getRemovalReason() != removalReason) {
            ctx.setArg(1, removeEvent.getRemovalReason());
        }
    }

    @Hook(
            cls = "net/minecraft/world/entity/LivingEntity",
            method = "tick",
            descriptor = "()V"
    )
    public static void onLivingTick(HookContext ctx) {
        ctx.post(new LivingTickEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/world/entity/Entity",
            method = "tick",
            descriptor = "()V"
    )
    public static void onEntityTick(HookContext ctx) {
        ctx.post(new EntityTickEvent(ctx.getSelf()));
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerPlayerGameMode",
            method = "useItem",
            descriptor = "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
    )
    public static void onPlayerUseItem(HookContext ctx) {
        ServerPlayer player = ctx.getArg(1);
        Level level = ctx.getArg(2);
        ItemStack stack = ctx.getArg(3);
        InteractionHand hand = ctx.getArg(4);
        PlayerUseItemEvent event = new PlayerUseItemEvent(player, level, stack, hand);
        ctx.post(event);
    }

    @Hook(
            cls = "net/minecraft/server/level/ServerPlayerGameMode",
            method = "setGameModeForPlayer",
            descriptor = "(Lnet/minecraft/world/level/GameType;Lnet/minecraft/world/level/GameType;)V"
    )
    public static void onSetGameModeForPlayer(HookContext ctx) {
        GameType gameType = ctx.getArg(1);
        GameType previousGameModeForPlayer = ctx.getArg(2);
        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(ReflectUtil.getField(ctx.getSelf(), "player"),
                gameType, previousGameModeForPlayer);
        ctx.post(event);
        if (event.getPreviousGameModeForPlayer() != previousGameModeForPlayer) {
            ctx.setArg(2, event.getPreviousGameModeForPlayer());
        }
    }

    @Hook(
            cls = "net/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil",
            method = "renderTooltipBackground",
            descriptor = "(Lnet/minecraft/client/gui/GuiGraphics;IIIILnet/minecraft/resources/Identifier;)V"
    )
    public static void onRenderTooltipBackground(HookContext ctx) {
        GuiGraphics guiGraphics = ctx.getArg(1);
        int x = ctx.getArg(2);
        int y = ctx.getArg(3);
        int width = ctx.getArg(4);
        int height = ctx.getArg(5);
        Identifier resource = ctx.getArg(6);
        RenderTooltipBackgroundEvent event = new RenderTooltipBackgroundEvent(guiGraphics, x, y, width, height, resource);
        ctx.post(event);
    }

    @Hook(
            cls = "net/minecraft/client/gui/GuiGraphics",
            method = "renderTooltip",
            descriptor = "(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V"
    )
    public static void onRenderTooltip(HookContext ctx) {
        Font font = ctx.getArg(1);
        List<ClientTooltipComponent> lines = ctx.getArg(2);
        int mouseX = ctx.getArg(3);
        int mouseY = ctx.getArg(4);
        ItemStack hoveredItem = WModUtils.getHoveredItem(mouseX, mouseY);
        if (!hoveredItem.isEmpty()) {
            RenderTooltipEvent event = new RenderTooltipEvent(ctx.getSelf(), hoveredItem, lines, mouseX, mouseY, font);
            ctx.post(event);
            if (event.isModified()) {
                ctx.setArg(1, event.getFont());
                ctx.setArg(2, event.getTooltipLines());
            }
        }
    }
}
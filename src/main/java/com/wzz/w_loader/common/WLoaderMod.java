package com.wzz.w_loader.common;

import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.annotation.Subscribe;
import com.wzz.w_loader.annotation.WMod;
import com.wzz.w_loader.command.WCommandBuilder;
import com.wzz.w_loader.event.events.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@WMod(name = "W Loader", modId = "w_loader")
public class WLoaderMod {

    @Subscribe
    public void onClientTick(ClientTickEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getWindow() != null) {
            // 在标题栏彰显 W-Loader 的存在感
            mc.getWindow().setTitle("Minecraft 26.1 - [W-Loader]");
        }
    }

    @Subscribe
    public static void registerCommand(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                WCommandBuilder.create("loader")
                        .requires(2)
                        .literal("entity", entityNode -> entityNode
                                .executes(context -> {
                                    scanEntities(context.getSource().getLevel(), context.getSource()::sendSystemMessage);
                                    return 1;
                                })
                        )
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
    private static void scanEntities(ServerLevel world, java.util.function.Consumer<Component> messenger) {
        try {
            messenger.accept(Component.literal("§6[W-Loader]§f 正在执行物理内存扫描..."));
            Field managerField = ServerLevel.class.getDeclaredField("entityManager");
            managerField.setAccessible(true);
            PersistentEntitySectionManager<?> entityManager = (PersistentEntitySectionManager<?>) managerField.get(world);
            Field lookUpField = PersistentEntitySectionManager.class.getDeclaredField("visibleEntityStorage");
            lookUpField.setAccessible(true);
            EntityLookup<?> entityLookup = (EntityLookup<?>) lookUpField.get(entityManager);
            Field byIdField = EntityLookup.class.getDeclaredField("byId");
            byIdField.setAccessible(true);
            Int2ObjectMap<EntityAccess> byIdMap = (Int2ObjectMap<EntityAccess>) byIdField.get(entityLookup);
            Field byUuidField = EntityLookup.class.getDeclaredField("byUuid");
            byUuidField.setAccessible(true);
            Map<UUID, EntityAccess> byUuidMap = (Map<UUID, EntityAccess>) byUuidField.get(entityLookup);
            messenger.accept(Component.literal("§b▶ byIdMap 统计: §e" + byIdMap.size() + "§f 个实体"));
            byIdMap.forEach((id, access) -> {
                if (access instanceof Entity entity) {
                    String info = String.format(" §8- §7[%d] §f%s §8(%s)", id, entity.getType().toString(), entity.getUUID());
                    messenger.accept(Component.literal(info));
                }
            });
            messenger.accept(Component.literal("§b▶ byUuidMap 统计: §e" + byUuidMap.size() + "§f 个实体"));
            byUuidMap.forEach((uuid, access) -> {
                if (access instanceof Entity entity) {
                    String info = String.format(" §8- §7UUID: §f%s §8(ID: %d)", uuid, entity.getId());
                    messenger.accept(Component.literal(info));
                }
            });

            messenger.accept(Component.literal("§6[W-Loader]§a 扫描完成。"));

        } catch (Exception e) {
            messenger.accept(Component.literal("§c[W-Loader] 物理扫描失败: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
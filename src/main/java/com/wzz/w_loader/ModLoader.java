package com.wzz.w_loader;

import com.wzz.w_loader.event.EventBus;
import com.wzz.w_loader.event.events.CreativeTabEvent;
import com.wzz.w_loader.event.events.CreativeTabEventCache;
import com.wzz.w_loader.event.events.RegistryEvent;
import com.wzz.w_loader.logger.WLogger;
import com.wzz.w_loader.registry.Registries;
import com.wzz.w_loader.resource.ModResourceManager;
import com.wzz.w_loader.resource.ModResourcePack;

import java.util.ArrayList;
import java.util.List;

public final class ModLoader {

    public static final ModLoader INSTANCE = new ModLoader();
    private final List<PendingMod> pendingMods = new ArrayList<>();
    private final List<LoadedMod> loadedMods   = new ArrayList<>();

    private ModLoader() {}

    public void addPending(PendingMod mod) { pendingMods.add(mod); }
    public List<PendingMod> getPendingMods() { return pendingMods; }
    public List<LoadedMod>  getLoadedMods()  { return loadedMods; }

    /**
     * 在 Bootstrap.bootStrap() atTail 调用
     * 此时 MC 类全部就绪，可以 loadClass + newInstance
     * 同时触发 RegistryEvent，freeze 注册表
     */
    public static void onBootstrap() {
        WLogger.info("[ModLoader] Bootstrap phase: loading mod classes...");
        for (PendingMod pending : INSTANCE.pendingMods) {
            try {
                String modId = pending.metadata().name != null
                        ? pending.metadata().name
                        : pending.metadata().name();
                ModResourceManager.INSTANCE.addPack(
                        new ModResourcePack(modId, pending.jarFile()));
                Class<?> clazz    = pending.classLoader().loadClass(pending.metadata().main());
                Object   instance = clazz.getDeclaredConstructor().newInstance();
                EventBus.INSTANCE.register(instance);
                INSTANCE.loadedMods.add(new LoadedMod(instance, pending.metadata(), pending.classLoader()));
                WLogger.info("[ModLoader] Loaded: " + pending.metadata().name());
            } catch (Exception e) {
                WLogger.error("[ModLoader] Failed: " + pending.metadata().name());
            }
        }
        ModResourceManager.INSTANCE.addPack(new ModResourcePack("w_loader", null, true, ModLoader.class));
        INSTANCE.pendingMods.clear();

        EventBus.INSTANCE.post(new RegistryEvent());
        Registries.freezeAll();

        CreativeTabEvent tabEvent = new CreativeTabEvent();
        EventBus.INSTANCE.post(tabEvent);
        tabEvent.getPending().forEach(CreativeTabEventCache::put);
        WLogger.info("[ModLoader] Bootstrap phase complete.");
    }
}
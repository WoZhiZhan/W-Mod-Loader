package com.wzz.w_loader.internal;

import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.hook.HookManager;
import com.wzz.w_loader.hook.HookScanner;
import com.wzz.w_loader.hook.VanillaHooks;
import com.wzz.w_loader.transform.TransformerRegistry;
import com.wzz.w_loader.internal.transformer.UniversalTransformer;

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
            "net/minecraft/world/entity/LivingEntity",
            "net/minecraft/server/level/ServerPlayer",
            "net/minecraft/world/entity/Entity",
            "net/minecraft/client/gui/screens/Screen",
            "net/minecraft/client/gui/screens/TitleScreen",
            "net/minecraft/client/Minecraft",
            "net/minecraft/client/renderer/GameRenderer",
            "net/minecraft/server/level/ServerLevel",
            "net/minecraft/client/renderer/entity/EntityRenderers",
            "net/minecraft/client/multiplayer/ClientLevel",
            "net/minecraft/world/level/Level",
            "net/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil",
            "net/minecraft/client/gui/GuiGraphics",
            "net/minecraft/client/multiplayer/ClientPacketListener",
            "net/minecraft/CrashReport",
            "net/minecraft/client/gui/Gui",
            "net/minecraft/world/entity/item/ItemEntity",
            "net/minecraft/commands/Commands",
            "net/minecraft/server/MinecraftServer",
            "net/minecraft/world/inventory/EnchantmentMenu",
    };

    public static void registerAll() {
        HookScanner.preRegister(VanillaHooks.class);
        TransformerRegistry registry = TransformerRegistry.getInstance();
        for (String cls : HOOKED_CLASSES) {
            registry.register(new UniversalTransformer(cls));
        }
        HookManager.on("net/minecraft/core/registries/BuiltInRegistries")
                .method("createContents")
                .atTail()
                .inject(ctx -> {
                    HookScanner.bindCallbacks(VanillaHooks.class);
                    ModLoader.onBootstrap();
                });
    }

    private InternalTransformers() {}
}
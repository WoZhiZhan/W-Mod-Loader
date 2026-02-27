package com.wzz.w_loader.bootstrap;

import com.wzz.w_loader.ModLoader;
import com.wzz.w_loader.ModManager;
import com.wzz.w_loader.ModMetadata;
import com.wzz.w_loader.PendingMod;
import com.wzz.w_loader.internal.InternalTransformers;
import com.wzz.w_loader.internal.transformer.AccessTransformer;
import com.wzz.w_loader.logger.WLogger;
import com.wzz.w_loader.transform.TransformerRegistry;
import com.wzz.w_loader.transform.WClassTransformer;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;

public class Bootstrap {
    public static final String version = "1.2";
    private static Instrumentation INSTRUMENTATION;
    public static void premain(String args, Instrumentation instrumentation) {
        INSTRUMENTATION = instrumentation;
        WLogger.info("========== W Loader Starting ==========");
        instrumentation.addTransformer(new WClassTransformer(), true);
        try (InputStream is = Bootstrap.class.getResourceAsStream("/META-INF/at.cfg")) {
            if (is != null) {
                AccessTransformer.load(is);
                TransformerRegistry.getInstance().register(new AccessTransformer());
            }
        } catch (Exception e) {
            WLogger.error("Failed to load at.cfg: " + e.getMessage());
            e.printStackTrace();
        }
        InternalTransformers.registerAll();
        File modDir = new File("mods");
        if (!modDir.exists()) modDir.mkdirs();
        ModManager modManager = new ModManager();
        registerSelfAsMod();
        modManager.scanMods(modDir);
        WLogger.info("Premain done. Waiting for Minecraft to initialize...");
    }

    private static void registerSelfAsMod() {
        try {
            ModMetadata selfMeta = new ModMetadata();
            selfMeta.name = "W Loader";
            selfMeta.modId = "w_loader";
            selfMeta.version = version;
            selfMeta.main = "com.wzz.w_loader.common.WLoaderMod";
            selfMeta.description = "W Mod Loader - A lightweight Minecraft mod loader";
            URL[] urls = new URL[]{
                    Bootstrap.class.getProtectionDomain().getCodeSource().getLocation()
            };
            URLClassLoader selfLoader = new URLClassLoader(
                    urls,
                    Thread.currentThread().getContextClassLoader()
            );
            ModLoader.INSTANCE.addPending(new PendingMod(selfMeta, selfLoader, null));
            WLogger.info("Registered self as mod: W Loader");
        } catch (Exception e) {
            WLogger.error("Failed to register self as mod");
            e.printStackTrace();
        }
    }
    public static Instrumentation getINSTRUMENTATION() {
        return INSTRUMENTATION;
    }
}
package com.wzz.w_loader.bootstrap;

import com.wzz.w_loader.ModManager;
import com.wzz.w_loader.internal.InternalTransformers;
import com.wzz.w_loader.logger.WLogger;
import com.wzz.w_loader.transform.WClassTransformer;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class Bootstrap {

    public static void premain(String args, Instrumentation instrumentation) {
        WLogger.info("========== W Loader Starting ==========");
        instrumentation.addTransformer(new WClassTransformer(), true);
        InternalTransformers.registerAll();
        File modDir = new File("mods");
        if (!modDir.exists()) modDir.mkdirs();
        ModManager modManager = new ModManager();
        modManager.scanMods(modDir);
        WLogger.info("[BootStrop] Premain done. Waiting for Minecraft to initialize...");
    }
}
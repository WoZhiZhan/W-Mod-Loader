package com.wzz.w_loader;

import com.wzz.w_loader.event.EventBus;
import com.wzz.w_loader.logger.WLogger;
import com.wzz.w_loader.resource.ModResourceManager;
import com.wzz.w_loader.resource.ModResourcePack;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.*;

public class ModManager {

    public void scanMods(File modDirectory) {
        File[] files = modDirectory.listFiles((d, n) -> n.endsWith(".jar"));
        if (files == null || files.length == 0) {
            WLogger.info("[ModManager] No mods found in: " + modDirectory.getPath());
            return;
        }

        for (File file : files) {
            try {
                scanSingleMod(file);
            } catch (Exception e) {
                WLogger.error("[ModManager] Failed to scan: " + file.getName());
                e.printStackTrace();
            }
        }
    }

    private void scanSingleMod(File file) throws Exception {
        ModMetadata meta = readMetadata(file);
        if (meta == null) return;

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{ file.toURI().toURL() },
                Thread.currentThread().getContextClassLoader()
        );

        ModLoader.INSTANCE.addPending(new PendingMod(meta, classLoader, file));
        WLogger.info("[ModManager] Queued: " + meta.name());
    }

    private ModMetadata readMetadata(File jar) {
        try (JarFile jf = new JarFile(jar)) {
            JarEntry entry = jf.getJarEntry("mod.json");
            if (entry == null) return null;
            try (InputStream is = jf.getInputStream(entry)) {
                String json = new String(is.readAllBytes());
                ModMetadata meta = new ModMetadata();
                meta.name    = extractJson(json, "name");
                meta.version = extractJson(json, "version");
                meta.main    = extractJson(json, "main");
                if (meta.main == null || meta.main.isBlank()) return null;
                return meta;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private String extractJson(String json, String key) {
        String k = "\"" + key + "\"";
        int i = json.indexOf(k);      if (i < 0) return null;
        int c = json.indexOf(':', i); if (c < 0) return null;
        int s = json.indexOf('"', c + 1); if (s < 0) return null;
        int e = json.indexOf('"', s + 1); if (e < 0) return null;
        return json.substring(s + 1, e);
    }
}
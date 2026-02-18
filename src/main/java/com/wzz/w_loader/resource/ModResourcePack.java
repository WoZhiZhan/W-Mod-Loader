package com.wzz.w_loader.resource;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ModResourcePack extends AbstractPackResources {

    private final File jarFile;
    private final String modId;

    public ModResourcePack(String modId, File jarFile) {
        super(new PackLocationInfo(
                modId,
                Component.literal(modId),
                PackSource.DEFAULT,
                Optional.empty()
        ));
        this.modId = modId;
        this.jarFile = jarFile;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        String path = String.join("/", paths);
        return getJarEntry(path);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.Identifier loc) {
        String path = type.getDirectory() + "/" + loc.getNamespace() + "/" + loc.getPath();
        return getJarEntry(path);
    }

    @Override
    public String packId() {
        return modId;
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix,
                               ResourceOutput output) {
        String dirPrefix = type.getDirectory() + "/" + namespace + "/" + prefix;
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(dirPrefix) && !entry.isDirectory()) {
                    String remaining = name.substring(
                            (type.getDirectory() + "/" + namespace + "/").length());
                    net.minecraft.resources.Identifier loc =
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, remaining);
                    output.accept(loc, getJarEntry(name));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            String prefix = type.getDirectory() + "/";
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith(prefix)) {
                    String rest = name.substring(prefix.length());
                    int slash = rest.indexOf('/');
                    if (slash > 0) namespaces.add(rest.substring(0, slash));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return namespaces;
    }

    private JarFile openedJar = null;

    private JarFile getJar() throws Exception {
        if (openedJar == null) {
            openedJar = new JarFile(jarFile);
        }
        return openedJar;
    }

    @Override
    public void close() {
        if (openedJar != null) {
            try { openedJar.close(); } catch (Exception ignored) {}
            openedJar = null;
        }
    }

    private IoSupplier<InputStream> getJarEntry(String path) {
        try {
            JarFile jar = getJar();
            JarEntry entry = jar.getJarEntry(path);
            if (entry == null) return null;
            return () -> jar.getInputStream(entry);
        } catch (Exception e) {
            return null;
        }
    }
}
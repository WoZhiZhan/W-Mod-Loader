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
    private final boolean isBuiltin;
    private final Class<?> sourceClass;  // 内置 Mod 的资源来源类
    private JarFile openedJar;
    private final Map<String, IoSupplier<InputStream>> resourceSuppliers = new HashMap<>();

    public ModResourcePack(String modId, File jarFile) {
        this(modId, jarFile, false, null);
    }

    public ModResourcePack(String modId, File jarFile, boolean isBuiltin, Class<?> sourceClass) {
        super(new PackLocationInfo(
                modId,
                Component.literal(modId),
                PackSource.DEFAULT,
                Optional.empty()
        ));
        this.modId = modId;
        this.jarFile = jarFile;
        this.isBuiltin = isBuiltin;
        this.sourceClass = sourceClass;
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... paths) {
        String path = String.join("/", paths);
        return getResourceStream(path);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, net.minecraft.resources.Identifier loc) {
        String path = type.getDirectory() + "/" + loc.getNamespace() + "/" + loc.getPath();
        return getResourceStream(path);
    }

    @Override
    public String packId() {
        return modId;
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix,
                              ResourceOutput output) {
        if (isBuiltin) {
            listBuiltinResources(type, namespace, prefix, output);
        } else {
            listJarResources(type, namespace, prefix, output);
        }
    }

    private void listBuiltinResources(PackType type, String namespace, String prefix,
                                      ResourceOutput output) {
        String basePath = "/" + type.getDirectory() + "/" + namespace + "/" + prefix;
        try {
            String resourcePath = basePath.replace("/./", "/");
            InputStream is = sourceClass.getResourceAsStream(resourcePath);
            if (is != null) {
                String remaining = resourcePath.substring(
                        (type.getDirectory() + "/" + namespace + "/").length() + 1);
                net.minecraft.resources.Identifier loc =
                        net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, remaining);
                output.accept(loc, () -> is);
            }
        } catch (Exception _) {
        }
    }

    private void listJarResources(PackType type, String namespace, String prefix,
                                  ResourceOutput output) {
        try {
            getJar();
            String dirPrefix = type.getDirectory() + "/" + namespace + "/" + prefix;
            for (Map.Entry<String, IoSupplier<InputStream>> entry : resourceSuppliers.entrySet()) {
                String name = entry.getKey();
                if (name.startsWith(dirPrefix)) {
                    String remaining = name.substring(
                            (type.getDirectory() + "/" + namespace + "/").length());
                    net.minecraft.resources.Identifier loc =
                            net.minecraft.resources.Identifier.fromNamespaceAndPath(namespace, remaining);
                    output.accept(loc, entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private IoSupplier<InputStream> getResourceStream(String path) {
        if (isBuiltin) {
            InputStream is = sourceClass.getResourceAsStream("/" + path);
            return is != null ? () -> is : null;
        } else {
            try {
                getJar();
                return resourceSuppliers.get(path);
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        if (isBuiltin) {
            return getBuiltinNamespaces(type);
        } else {
            return getJarNamespaces(type);
        }
    }

    private Set<String> getBuiltinNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        namespaces.add(modId);
        return namespaces;
    }

    private Set<String> getJarNamespaces(PackType type) {
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

    private synchronized JarFile getJar() throws Exception {
        if (openedJar == null && !isBuiltin) {
            openedJar = new JarFile(jarFile);
            Enumeration<JarEntry> entries = openedJar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory()) {
                    String path = entry.getName();
                    resourceSuppliers.put(path, () -> openedJar.getInputStream(entry));
                }
            }
        }
        return openedJar;
    }

    @Override
    public void close() {
        if (openedJar != null) {
            try { openedJar.close(); } catch (Exception ignored) {}
            openedJar = null;
            resourceSuppliers.clear();
        }
    }
}
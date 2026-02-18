package com.wzz.w_loader.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.*;
import java.util.function.Supplier;

public class Registry<T> {

    private final String name;
    private final net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey;
    private final List<DeferredEntry<T>> deferred = new ArrayList<>();
    private final Map<String, T> registered = new LinkedHashMap<>();
    private boolean frozen = false;

    private Registry(String name, net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registryKey) {
        this.name = name;
        this.registryKey = registryKey;
    }

    public static Registry<Item> items() {
        return new Registry<>("items", BuiltInRegistries.ITEM.key());
    }

    public static Registry<Block> blocks() {
        return new Registry<>("blocks", BuiltInRegistries.BLOCK.key());
    }

    public static Registry<CreativeModeTab> tabs() {
        return new Registry<>("tabs", BuiltInRegistries.CREATIVE_MODE_TAB.key());
    }

    /**
     * 生成 ResourceKey，供 Item.Properties.setId() 使用
     * 用法：new Item.Properties().setId(Registries.ITEMS.key("mymod:my_item"))
     */
    public ResourceKey<T> key(String id) {
        return ResourceKey.create(registryKey, Identifier.parse(id));
    }

    public void register(String id, Supplier<T> supplier) {
        if (frozen) throw new IllegalStateException("Registry " + name + " is already frozen!");
        deferred.add(new DeferredEntry<>(id, supplier));
    }

    public void freeze(RegistryConsumer<T> consumer) {
        if (frozen) return;
        for (DeferredEntry<T> entry : deferred) {
            try {
                T value = entry.supplier.get();
                consumer.accept(entry.id, value);
                registered.put(entry.id, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        frozen = true;
        deferred.clear();
    }

    public Optional<T> get(String id) { return Optional.ofNullable(registered.get(id)); }
    public Map<String, T> getAll() { return Collections.unmodifiableMap(registered); }

    @FunctionalInterface
    public interface RegistryConsumer<T> {
        void accept(String id, T value) throws Exception;
    }

    private record DeferredEntry<T>(String id, Supplier<T> supplier) {}
}
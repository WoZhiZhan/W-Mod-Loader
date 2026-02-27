package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.List;

public class EnchantmentEvent extends Event {
    // 核心参数（匹配 Hook 传递的 5 个参数）
    private final RegistryAccess registryAccess;
    private final ItemStack enchantItem;
    private final int enchantSlot;
    private final int enchantCost;
    private List<EnchantmentInstance> modifiedEnchantList;

    /**
     * 构造方法：接收 5 个参数
     * @param registryAccess 注册表访问器（1.26.1 测试版核心）
     * @param enchantItem 附魔物品
     * @param enchantSlot 附魔槽位
     * @param enchantCost 附魔等级消耗
     * @param originalEnchantList 原版生成的附魔列表
     */
    public EnchantmentEvent(RegistryAccess registryAccess,
                            ItemStack enchantItem, int enchantSlot, int enchantCost,
                            List<EnchantmentInstance> originalEnchantList) {
        this.registryAccess = registryAccess;
        this.enchantItem = enchantItem;
        this.enchantSlot = enchantSlot;
        this.enchantCost = enchantCost;
        this.modifiedEnchantList = originalEnchantList;
    }
    public List<EnchantmentInstance> getModifiedEnchantList() {
        return modifiedEnchantList;
    }

    public void setModifiedEnchantList(List<EnchantmentInstance> modifiedEnchantList) {
        this.modifiedEnchantList = modifiedEnchantList;
    }

    @Override
    public boolean isCancellable() {
        return true; // 标记事件可取消
    }

    public RegistryAccess getRegistryAccess() {
        return registryAccess;
    }

    public ItemStack getEnchantItem() {
        return enchantItem;
    }

    public int getEnchantSlot() {
        return enchantSlot;
    }

    public int getEnchantCost() {
        return enchantCost;
    }
}
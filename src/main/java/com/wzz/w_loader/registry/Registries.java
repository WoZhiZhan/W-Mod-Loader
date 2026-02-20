package com.wzz.w_loader.registry;

import com.wzz.w_loader.event.EventBus;
import com.wzz.w_loader.event.events.EntityAttributeCreationEvent;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashMap;
import java.util.Map;

public final class Registries {
    public static final Registry<Item>           ITEMS  = Registry.items();
    public static final Registry<Block>          BLOCKS = Registry.blocks();
    public static final Registry<EntityType<?>> ENTITIES = Registry.entities();
    public static final Registry<CreativeModeTab> TABS  = Registry.tabs();
    public static final Registry<MobEffect> MOB_EFFECTS  = Registry.mobEffects();
    public static final Registry<MenuType<?>> MENUS  = Registry.menus();
    public static final Registry<ParticleType<?>> PARTICLES  = Registry.particles();
    public static final Registry<BlockEntityType<?>> BLOCK_ENTITIES  = Registry.blockEntities();
    public static final Registry<Attribute> ATTRIBUTES  = Registry.attributes();
    public static final Registry<RecipeType<?>> RECIPES  = Registry.recipes();
    public static final Registry<SoundEvent> SOUNDS  = Registry.sounds();

    private static final Map<EntityType<? extends LivingEntity>, AttributeSupplier> ENTITY_ATTRIBUTES = new HashMap<>();

    public static void freezeAll() {
        ENTITIES.freeze((id, entityType) -> {
            net.minecraft.core.Registry.register(
                    BuiltInRegistries.ENTITY_TYPE,
                    Identifier.parse(id),
                    entityType
            );
        });
        fireEntityAttributeCreationEvent();
        ITEMS.freeze((id, item) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.ITEM, Identifier.parse(id), item));
        BLOCKS.freeze((id, block) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK, Identifier.parse(id), block));
        TABS.freeze((id, tab) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.parse(id), tab));
        MOB_EFFECTS.freeze((id, mobEffect) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.MOB_EFFECT, Identifier.parse(id), mobEffect));
        MENUS.freeze((id, menuType) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.MENU, Identifier.parse(id), menuType));
        PARTICLES.freeze((id, particleType) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.PARTICLE_TYPE, Identifier.parse(id), particleType));
        BLOCK_ENTITIES.freeze((id, blockEntityType) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Identifier.parse(id), blockEntityType));
        ATTRIBUTES.freeze((id, attribute) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.ATTRIBUTE, Identifier.parse(id), attribute));
        RECIPES.freeze((id, recipeType) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.RECIPE_TYPE, Identifier.parse(id), recipeType));
        SOUNDS.freeze((id, soundEvent) ->
                net.minecraft.core.Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.parse(id), soundEvent));
    }

    @SuppressWarnings("unchecked")
    private static void fireEntityAttributeCreationEvent() {
        Map<EntityType<? extends LivingEntity>, AttributeSupplier> eventMap = new HashMap<>();
        for (Map.Entry<String, EntityType<?>> entry : ENTITIES.getAll().entrySet()) {
            EntityType<?> type = entry.getValue();
            if (isLivingEntityType(type)) {
                EntityType<? extends LivingEntity> livingType = (EntityType<? extends LivingEntity>) type;
                if (livingType.getCategory() != MobCategory.MISC) {
                    eventMap.put(livingType, null);
                }
            }
        }
        EntityAttributeCreationEvent event = new EntityAttributeCreationEvent(eventMap);
        EventBus.INSTANCE.post(event);
        for (Map.Entry<EntityType<? extends LivingEntity>, AttributeSupplier> entry : eventMap.entrySet()) {
            EntityType<? extends LivingEntity> type = entry.getKey();
            AttributeSupplier supplier = entry.getValue();
            if (supplier == null) {
                throw new IllegalStateException(
                        "Entity " + BuiltInRegistries.ENTITY_TYPE.getKey(type) +
                                " (category: " + type.getCategory() + ") has no attributes registered! " +
                                "Did you forget to register attributes in EntityAttributeCreationEvent?"
                );
            }
            ENTITY_ATTRIBUTES.put(type, supplier);
        }
    }

    private static boolean isLivingEntityType(EntityType<?> type) {
        Class<?> entityClass = type.getBaseClass();
        return entityClass != null && LivingEntity.class.isAssignableFrom(entityClass);
    }

    public static boolean hasAttributes(EntityType<?> type) {
        return ENTITY_ATTRIBUTES.containsKey(type);
    }
}
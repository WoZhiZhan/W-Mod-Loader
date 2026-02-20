package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.lang.reflect.Method;

public class EntityRegisterRenderersEvent extends Event {
    public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> entityType,
                                                          EntityRendererProvider<T> entityRendererProvider) {
        try {
            Method method = EntityRenderers.class.getDeclaredMethod("register", EntityType.class, EntityRendererProvider.class);
            method.setAccessible(true);
            method.invoke(null, entityType, entityRendererProvider);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends BlockEntity, S extends BlockEntityRenderState> void registerBlockEntityRenderer(
            BlockEntityType<? extends T> blockEntityType,
            BlockEntityRendererProvider<T, S> blockEntityRendererProvider) {
        try {
            Method method = BlockEntityRenderers.class.getDeclaredMethod("register", BlockEntityType.class, BlockEntityRendererProvider.class);
            method.setAccessible(true);
            method.invoke(null, blockEntityType, blockEntityRendererProvider);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

package com.wzz.w_loader.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wzz.w_loader.event.Event;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.Entity;

public class EntityRenderEvent extends Event {
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final Entity crosshairPickEntity;
    private final EntityRenderState entityRenderState;
    private final CameraRenderState cameraRenderState;
    private final double x, y, z;
    private final PoseStack poseStack;
    private final SubmitNodeCollector submitNodeCollector;

    public EntityRenderEvent(EntityRenderDispatcher renderDispatcher, Entity crosshairPickEntity, EntityRenderState entityRenderState, CameraRenderState cameraRenderState, double x,
                             double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector) {
        this.entityRenderDispatcher = renderDispatcher;
        this.crosshairPickEntity = crosshairPickEntity;
        this.entityRenderState = entityRenderState;
        this.cameraRenderState = cameraRenderState;
        this.x = x;
        this.y = y;
        this.z = z;
        this.poseStack = poseStack;
        this.submitNodeCollector = submitNodeCollector;
    }

    public EntityRenderDispatcher getEntityRenderDispatcher() {
        return entityRenderDispatcher;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public CameraRenderState getCameraRenderState() {
        return cameraRenderState;
    }

    public Entity getCrosshairPickEntity() {
        return crosshairPickEntity;
    }

    public EntityRenderState getEntityRenderState() {
        return entityRenderState;
    }

    public PoseStack getPoseStack() {
        return poseStack;
    }

    public SubmitNodeCollector getSubmitNodeCollector() {
        return submitNodeCollector;
    }

    @Override
    public boolean isCancellable() {
        return true;
    }
}

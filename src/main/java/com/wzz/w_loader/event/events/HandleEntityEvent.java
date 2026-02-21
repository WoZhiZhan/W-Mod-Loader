package com.wzz.w_loader.event.events;

import com.wzz.w_loader.event.Event;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public class HandleEntityEvent extends Event {
    private final Entity entity;
    private final int packId;
    private final Pack pack;

    public HandleEntityEvent(Entity entity, int packId) {
        this.entity = entity;
        this.packId = packId;
        this.pack = Pack.getPack(packId);
    }

    public int getPackId() {
        return packId;
    }

    public Entity getEntity() {
        return entity;
    }

    @Nullable
    public Pack getPack() {
        return pack;
    }

    public enum Pack {
        GUARDIAN_ATTACK(21),
        TOTEM_TRIGGER(35),
        SNIFFER_SOUND(63);

        final int id;

        Pack(int id) {
            this.id = id;
        }

        public static Pack getPack(int id) {
            if (id == 21)
                return GUARDIAN_ATTACK;
            if (id == 35)
                return TOTEM_TRIGGER;
            if (id == 63)
                return SNIFFER_SOUND;
            return null;
        }
    }
}

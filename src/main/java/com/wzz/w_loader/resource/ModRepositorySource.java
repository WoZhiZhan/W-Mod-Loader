package com.wzz.w_loader.resource;

import com.wzz.w_loader.logger.WLogger;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;

import java.util.function.Consumer;

public class ModRepositorySource implements RepositorySource {

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        for (ModResourcePack pack : ModResourceManager.INSTANCE.getPacks()) {
            Pack wrappedPack = Pack.readMetaAndCreate(
                    new PackLocationInfo(
                            pack.packId(),
                            net.minecraft.network.chat.Component.literal(pack.packId()),
                            PackSource.DEFAULT,
                            java.util.Optional.empty()
                    ),
                    new Pack.ResourcesSupplier() {
                        @Override
                        public net.minecraft.server.packs.PackResources openPrimary(
                                PackLocationInfo info) {
                            return pack;
                        }

                        @Override
                        public net.minecraft.server.packs.PackResources openFull(
                                PackLocationInfo info,
                                Pack.Metadata metadata) {
                            return pack;
                        }
                    },
                    PackType.CLIENT_RESOURCES,
                    new PackSelectionConfig(
                            true, Pack.Position.TOP, true)
            );

            if (wrappedPack != null) {
                consumer.accept(wrappedPack);
                WLogger.info("[ModRepositorySource] Loaded pack: " + pack.packId());
            }
        }
    }
}
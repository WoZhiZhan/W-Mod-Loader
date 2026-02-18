package com.wzz.w_loader.resource;

import java.util.ArrayList;
import java.util.List;

public final class ModResourceManager {
    public static final ModResourceManager INSTANCE = new ModResourceManager();
    private final List<ModResourcePack> packs = new ArrayList<>();
    private ModResourceManager() {}

    public void addPack(ModResourcePack pack) { packs.add(pack); }
    public List<ModResourcePack> getPacks() { return packs; }
}
package com.wzz.w_loader;

import java.net.URLClassLoader;

/** Phase 2 完成后：持有真实实例 */
public record LoadedMod(Object instance, ModMetadata metadata, URLClassLoader classLoader) {}
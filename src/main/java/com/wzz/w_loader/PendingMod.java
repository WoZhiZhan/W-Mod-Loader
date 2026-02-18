package com.wzz.w_loader;

import java.io.File;
import java.net.URLClassLoader;

public record PendingMod(ModMetadata metadata, URLClassLoader classLoader, File jarFile) {}
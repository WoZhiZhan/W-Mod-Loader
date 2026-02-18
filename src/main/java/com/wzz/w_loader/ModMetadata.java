package com.wzz.w_loader;

public class ModMetadata {
    public String name;
    public String version;
    public String main;

    @Override
    public String toString() {
        return name + "@" + version;
    }

    public String name() {
        return name;
    }

    public String main() {
        return main;
    }

    public String version() {
        return version;
    }
}
package com.wzz.w_loader;

public class ModMetadata {
    public String name;
    public String version;
    public String modId;
    public String main;
    public String description;

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

    public String modId() {
        return modId;
    }

    public String description() {
        return description;
    }
}
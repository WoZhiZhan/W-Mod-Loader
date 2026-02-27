package com.wzz.w_loader.util;

public class TestDescriptor {
    public static void main(String[] args) {
        String descriptor = MethodDescriptorGenerator.getMethodDescriptorFromBytecode(
            "net/minecraft/client/renderer/entity/EntityRenderDispatcher",
            "submit"
        );
        System.out.println("方法描述符: " + descriptor);
    }
}
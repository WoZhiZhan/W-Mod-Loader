package com.wzz.w_loader.util;

import com.wzz.w_loader.internal.library.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

public class MethodDescriptorGenerator {
    
    /**
     * 通过类路径和方法名获取 JVM 方法描述符
     * @param className 类路径 (如: "net/minecraft/server/level/ServerLevel")
     * @param methodName 方法名
     * @param paramTypes 参数类型数组 (可选，用于重载方法的区分)
     * @return JVM 方法描述符
     */
    public static String getMethodDescriptor(String className, String methodName, Class<?>... paramTypes) {
        try {
            Class<?> clazz = Class.forName(className.replace('/', '.'));
            Method method = findMethod(clazz, methodName, paramTypes);
            if (method != null) {
                return getMethodDescriptor(method);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 通过 ASM 从字节码中获取方法描述符
     * @param className 类路径 (如: "net/minecraft/server/level/ServerLevel")
     * @param methodName 方法名
     * @param descriptor 已知的部分描述符 (可选)
     * @return JVM 方法描述符
     */
    public static String getMethodDescriptorFromBytecode(String className, String methodName, String... descriptor) {
        try {
            // 将类路径转换为资源路径
            String resourcePath = "/" + className + ".class";
            InputStream inputStream = MethodDescriptorGenerator.class.getResourceAsStream(resourcePath);
            
            if (inputStream == null) {
                // 尝试使用 ClassLoader
                inputStream = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(className + ".class");
            }
            
            if (inputStream != null) {
                ClassReader classReader = new ClassReader(inputStream);
                MethodDescriptorFinder finder = new MethodDescriptorFinder(methodName, descriptor);
                classReader.accept(finder, 0);
                inputStream.close();
                
                if (finder.foundDescriptor != null) {
                    return finder.foundDescriptor;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 从 Method 对象获取 JVM 描述符
     */
    public static String getMethodDescriptor(Method method) {
        StringBuilder descriptor = new StringBuilder("(");
        
        for (Class<?> paramType : method.getParameterTypes()) {
            descriptor.append(getTypeDescriptor(paramType));
        }
        
        descriptor.append(")").append(getTypeDescriptor(method.getReturnType()));
        return descriptor.toString();
    }
    
    /**
     * 获取类型的 JVM 描述符
     */
    public static String getTypeDescriptor(Class<?> type) {
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        if (type.isArray()) {
            return "[" + getTypeDescriptor(type.getComponentType());
        }
        return "L" + type.getName().replace('.', '/') + ";";
    }
    
    /**
     * 获取参数的 JVM 描述符字符串 (用于钩子注解)
     */
    public static String getParameterDescriptor(Class<?>... paramTypes) {
        StringBuilder descriptor = new StringBuilder("(");
        for (Class<?> paramType : paramTypes) {
            descriptor.append(getTypeDescriptor(paramType));
        }
        descriptor.append(")");
        return descriptor.toString();
    }
    
    /**
     * 查找方法（处理重载）
     */
    private static Method findMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        try {
            if (paramTypes.length > 0) {
                return clazz.getDeclaredMethod(methodName, paramTypes);
            } else {
                // 如果没有指定参数，返回第一个匹配的方法
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        return method;
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // 忽略，尝试父类
        }
        return null;
    }
    
    /**
     * ASM ClassVisitor 用于查找方法描述符
     */
    private static class MethodDescriptorFinder extends ClassVisitor {
        private final String methodName;
        private final String[] partialDescriptor;
        public String foundDescriptor = null;
        
        public MethodDescriptorFinder(String methodName, String... partialDescriptor) {
            super(Opcodes.ASM9);
            this.methodName = methodName;
            this.partialDescriptor = partialDescriptor;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, 
                                        String signature, String[] exceptions) {
            if (name.equals(methodName)) {
                // 如果提供了部分描述符，检查是否匹配
                if (partialDescriptor != null && partialDescriptor.length > 0) {
                    for (String desc : partialDescriptor) {
                        if (descriptor.contains(desc)) {
                            foundDescriptor = descriptor;
                            break;
                        }
                    }
                } else {
                    foundDescriptor = descriptor;
                }
            }
            return null;
        }
    }
}
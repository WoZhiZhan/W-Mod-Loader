package com.wzz.w_loader.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

public final class ReflectUtil {

    private static final ConcurrentHashMap<String, Field>  FIELD_CACHE  = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    private ReflectUtil() {}

    /**
     * 取字段值，会向父类查找
     * ReflectUtil.getField(packetListener, "player")
     */
    @SuppressWarnings("unchecked")
    public static <T> T getField(Object obj, String fieldName) {
        String key = obj.getClass().getName() + "#" + fieldName;
        Field field = FIELD_CACHE.computeIfAbsent(key, k -> findField(obj.getClass(), fieldName));
        if (field == null) throw new RuntimeException("Field not found: " + fieldName);
        try {
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 调用无参方法
     * ReflectUtil.invoke(packet, "message")
     */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object obj, String methodName) {
        String key = obj.getClass().getName() + "#" + methodName + "()";
        Method method = METHOD_CACHE.computeIfAbsent(key, k -> findMethod(obj.getClass(), methodName));
        if (method == null) throw new RuntimeException("Method not found: " + methodName);
        try {
            return (T) method.invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void setField(Object obj, String fieldName, Object value) {
        String key = obj.getClass().getName() + "#" + fieldName + "_set";
        Field field = FIELD_CACHE.computeIfAbsent(key, k -> findField(obj.getClass(), fieldName));
        if (field == null) throw new RuntimeException("Field not found: " + fieldName);
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field findField(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> clazz, String name) {
        Class<?> c = clazz;
        while (c != null) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    m.setAccessible(true);
                    return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    /** 调用有参方法 */
    @SuppressWarnings("unchecked")
    public static <T> T invoke(Object obj, String methodName, Object... args) {
        try {
            Method m = findMethodByName(obj.getClass(), methodName, args.length);
            m.setAccessible(true);
            return (T) m.invoke(obj, args);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invokeStatic(String className, String methodName, Object... args) {
        try {
            Class<?> cls = Class.forName(className);
            Method m = findMethodByName(cls, methodName, args.length);
            m.setAccessible(true);
            return (T) m.invoke(null, args);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static Method findMethodByName(Class<?> clazz, String name, int paramCount) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == paramCount) return m;
        }
        if (clazz.getSuperclass() != null) return findMethodByName(clazz.getSuperclass(), name, paramCount);
        throw new RuntimeException("Method not found: " + name);
    }
}
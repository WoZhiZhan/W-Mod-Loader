package com.wzz.w_loader.transform;

/**
 * Mod 实现此接口，在 transform() 中用 ASM 修改目标类字节码。
 */
public interface IClassTransformer {

    /**
     * @return 要拦截的类名（斜杠分隔），返回 null 表示接管所有类（如 AccessTransformer）
     */
    String targetClass();

    /**
     * 普通 transformer 使用，className 已由 WClassTransformer 保证匹配
     */
    byte[] transform(byte[] classBytes);

    /**
     * AT 等需要感知 className 的 transformer 覆盖此方法
     * 默认实现直接转发给 transform(byte[])
     */
    default byte[] transform(String className, byte[] classBytes) {
        return transform(classBytes);
    }
}
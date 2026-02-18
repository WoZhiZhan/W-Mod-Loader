package com.wzz.w_loader.transform;

/**
 * Mod 实现此接口，在 transform() 中用 ASM 修改目标类字节码。
 */
public interface IClassTransformer {

    /**
     * @return 要拦截的类名，格式：com/example/TargetClass（斜杠分隔）
     */
    String targetClass();

    /**
     * @param classBytes 原始字节码
     * @return 修改后的字节码，不修改则返回 classBytes
     */
    byte[] transform(byte[] classBytes);
}
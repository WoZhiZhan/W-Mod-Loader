package com.wzz.w_loader.transform;

import com.wzz.w_loader.logger.WLogger;

import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.List;

public class WClassTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(
            ClassLoader loader,
            String className,
            Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain,
            byte[] classfileBuffer) {

        if (className == null) return classfileBuffer;

        List<IClassTransformer> transformers = TransformerRegistry.getInstance().getTransformers();
        byte[] result = classfileBuffer;

        for (IClassTransformer transformer : transformers) {
            String target = transformer.targetClass();
            // target == null 表示该 transformer 处理所有类（如 AccessTransformer）
            if (target == null || className.equals(target)) {
                try {
                    result = transformer.transform(className, result);
                } catch (Throwable e) {
                    WLogger.error("[WClassTransformer] Failed to transform " + className
                            + " using " + transformer.getClass().getSimpleName());
                    e.printStackTrace();
                    return classfileBuffer;
                }
            }
        }

        return result;
    }
}
package com.wzz.w_loader.transform;

import java.util.ArrayList;
import java.util.List;

public class TransformerRegistry {

    private static final TransformerRegistry INSTANCE = new TransformerRegistry();
    private final List<IClassTransformer> transformers = new ArrayList<>();

    private TransformerRegistry() {}

    public static TransformerRegistry getInstance() {
        return INSTANCE;
    }

    public void register(IClassTransformer transformer) {
        transformers.add(transformer);
    }

    public List<IClassTransformer> getTransformers() {
        return transformers;
    }
}
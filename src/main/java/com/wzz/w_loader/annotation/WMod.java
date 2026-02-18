package com.wzz.w_loader.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface WMod {
    String modId();
    String name() default "";
    String version() default "1.0.0";
    String[] dependencies() default {};
}
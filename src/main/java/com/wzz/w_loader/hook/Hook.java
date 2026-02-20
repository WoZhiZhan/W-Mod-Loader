package com.wzz.w_loader.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Hook {
    String cls();
    String method();
    String descriptor() default "";
    HookPoint.Position at() default HookPoint.Position.HEAD;
}
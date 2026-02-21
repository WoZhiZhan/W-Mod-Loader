package com.wzz.w_loader.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Hook {
    /** 要注入的目标类（斜杠格式），例如 "net/minecraft/world/entity/LivingEntity" */
    String cls();

    /** 要注入的目标方法名 */
    String method();

    /** 要注入的目标方法描述符，空字符串=匹配所有同名方法 */
    String descriptor() default "";

    /** 注入位置：HEAD=方法头部，TAIL=方法尾部，INVOKE=方法内某次调用前 */
    HookPoint.Position at() default HookPoint.Position.HEAD;

    // ---- 以下三项仅 at=INVOKE 时有效 ----

    /** 被调用方法所在类（斜杠格式），空字符串=不限类 */
    String invokeOwner() default "";

    /** 被调用方法名 */
    String invokeMethod() default "";

    /** 被调用方法描述符，空字符串=不限描述符 */
    String invokeDesc() default "";
}

package com.mdm.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解：同一用户对同一接口在间隔窗口内的重复请求将被拒绝（42900）。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {

    /** 间隔窗口（毫秒），默认 3000 */
    int interval() default 3000;
}

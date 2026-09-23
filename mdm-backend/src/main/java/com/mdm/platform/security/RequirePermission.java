package com.mdm.platform.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色声明：拦截器校验当前用户角色是否在允许列表内。
 * 未标注的接口默认对全部已识别用户开放。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    Role[] value();
}

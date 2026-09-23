package com.mdm.platform.config;

import com.mdm.platform.security.PermissionInterceptor;
import com.mdm.platform.security.RepeatSubmitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 配置：CORS + 权限/防重复提交拦截器注册。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;
    private final RepeatSubmitInterceptor repeatSubmitInterceptor;

    public WebConfig(PermissionInterceptor permissionInterceptor,
                     RepeatSubmitInterceptor repeatSubmitInterceptor) {
        this.permissionInterceptor = permissionInterceptor;
        this.repeatSubmitInterceptor = repeatSubmitInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/api/**");
    }
}

package com.mdm.platform.security;

import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 权限拦截器：解析 X-User-* 请求头 → UserContext；校验 @RequirePermission。
 */
@Component
public class PermissionInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String userId = request.getHeader("X-User-Id");
        String userName = request.getHeader("X-User-Name");
        String roleText = request.getHeader("X-User-Role");
        if (userId == null || userId.isBlank()) {
            writeError(response, ErrorCode.UNAUTHENTICATED);
            return false;
        }
        Role role;
        try {
            role = Role.valueOf(roleText == null ? "" : roleText.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            writeError(response, ErrorCode.UNAUTHENTICATED);
            return false;
        }
        UserContext.set(new UserContext(userId, userName, role));

        RequirePermission permission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (permission == null) {
            return true;
        }
        for (Role allowed : permission.value()) {
            if (allowed == role) {
                return true;
            }
        }
        writeError(response, ErrorCode.FORBIDDEN);
        return false;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorCode)));
    }
}

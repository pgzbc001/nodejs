package com.mdm.platform.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdm.platform.common.ApiResponse;
import com.mdm.platform.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防重复提交拦截器：用户 + 接口 + 请求体指纹在窗口期内仅放行一次。
 */
@Component
public class RepeatSubmitInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Long> recentRequests = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        NoRepeatSubmit annotation = handlerMethod.getMethodAnnotation(NoRepeatSubmit.class);
        if (annotation == null || !"POST".equalsIgnoreCase(request.getMethod())
                && !"PUT".equalsIgnoreCase(request.getMethod()) && !"DELETE".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        UserContext ctx = UserContext.get();
        String key = (ctx == null ? request.getRemoteAddr() : ctx.getUserId())
                + ":" + request.getMethod() + ":" + request.getRequestURI();
        long now = System.currentTimeMillis();
        Long last = recentRequests.put(key, now);
        if (last != null && now - last < annotation.interval()) {
            recentRequests.remove(key, now);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error(ErrorCode.BAD_REQUEST, "操作过于频繁，请勿重复提交")));
            return false;
        }
        return true;
    }
}

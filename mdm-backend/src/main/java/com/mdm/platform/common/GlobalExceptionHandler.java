package com.mdm.platform.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理：统一转换为 ApiResponse 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Map<String, Object>> handleBusiness(BusinessException ex) {
        Map<String, Object> data = null;
        if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
            data = new HashMap<>(ex.getFieldErrors());
        }
        return new ApiResponse<>(ex.getErrorCode().getCode(), ex.getMessage(), data);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> fieldErrors.merge(fe.getField(), fe.getDefaultMessage(), (a, b) -> a + "; " + b));
        return new ApiResponse<>(ErrorCode.BAD_REQUEST.getCode(), ErrorCode.BAD_REQUEST.getMessage(), fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Void> handleUnknown(Exception ex) {
        log.error("unexpected error", ex);
        return ApiResponse.error(ErrorCode.SYSTEM_ERROR);
    }
}

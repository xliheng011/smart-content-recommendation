package com.example.backend.common;

import org.springframework.http.HttpStatus;

/**
 * 业务异常。
 *
 * 统一携带 HTTP 状态码，交给 GlobalExceptionHandler 转换成
 * 标准 ApiResponse 结构，避免把堆栈直接暴露给前端。
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException internal(String message) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return status.value();
    }
}

package com.example.backend.common;

/**
 * 统一 API 响应包装。
 *
 * <pre>
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": { ... },
 *   "timestamp": 1730000000000
 * }
 * </pre>
 *
 * code == 200 表示业务成功，其余取值与 HTTP 状态码语义保持一致，
 * 便于前端在拦截器里统一处理。
 */
public class ApiResponse<T> {

    /** 业务成功码 */
    public static final int CODE_SUCCESS = 200;

    private int code;

    private String message;

    private T data;

    private long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(CODE_SUCCESS, "success", data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(CODE_SUCCESS, "success", null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(CODE_SUCCESS, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}

package com.example.backend.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.stream.Collectors;

/**
 * 全局异常处理：把所有异常统一收敛成 ApiResponse 结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常（推荐使用）
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(
            ApiException ex
    ) {

        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(
                        ex.getCode(),
                        ex.getMessage()
                ));
    }

    /**
     * Spring 原生状态异常，兼容历史代码
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(
            ResponseStatusException ex
    ) {

        HttpStatus status = HttpStatus.resolve(
                ex.getStatusCode().value()
        );

        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        String reason = ex.getReason() == null
                ? status.getReasonPhrase()
                : ex.getReason();

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), reason));
    }

    /**
     * @Valid 校验失败
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining("；"));

        if (message.isBlank()) {
            message = "请求参数校验失败";
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, message));
    }

    /**
     * 方法参数约束校验失败
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex
    ) {

        String message = ex.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));

        if (message.isBlank()) {
            message = "请求参数不合法";
        }

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, message));
    }

    /**
     * 缺少必填请求参数
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(
            MissingServletRequestParameterException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        400,
                        "缺少必要参数：" + ex.getParameterName()
                ));
    }

    /**
     * 参数类型不匹配
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(
                        400,
                        "参数格式不正确：" + ex.getName()
                ));
    }

    /**
     * 请求体无法解析
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(
            HttpMessageNotReadableException ex
    ) {

        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(400, "请求体格式不正确"));
    }

    /**
     * 兜底
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex
    ) {

        log.error("未处理异常: {}", ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        500,
                        "服务器内部错误，请稍后重试"
                ));
    }
}

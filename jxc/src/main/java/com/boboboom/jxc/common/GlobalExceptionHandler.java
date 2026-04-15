package com.boboboom.jxc.common;

import com.boboboom.jxc.identity.application.auth.UnauthorizedException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INIT_DATA_MESSAGE = "数据初始化未完成，请联系管理员";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.fail(ex.getMessage()));
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleValidationException(Exception ex) {
        String message = buildValidationMessage(ex);
        return ResponseEntity.badRequest().body(ApiResponse.fail(message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("系统内部异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("系统繁忙，请稍后重试"));
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadSqlGrammar(BadSqlGrammarException ex) {
        Throwable root = ex.getRootCause();
        String rootMessage = root == null ? ex.getMessage() : root.getMessage();
        log.error("数据库SQL语法异常", ex);
        if (isPostgresSqlException(root) && rootMessage != null && rootMessage.contains("does not exist")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(INIT_DATA_MESSAGE));
        }
        if (rootMessage != null && rootMessage.contains("不存在")) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail(INIT_DATA_MESSAGE));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(INIT_DATA_MESSAGE));
    }

    private boolean isPostgresSqlException(Throwable root) {
        return root != null && "org.postgresql.util.PSQLException".equals(root.getClass().getName());
    }

    private String buildValidationMessage(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException methodEx) {
            return joinBindingErrors(methodEx.getBindingResult());
        }
        if (ex instanceof BindException bindEx) {
            return joinBindingErrors(bindEx.getBindingResult());
        }
        if (ex instanceof ConstraintViolationException violationEx) {
            Set<String> messages = violationEx.getConstraintViolations().stream()
                    .map(item -> {
                        String path = item.getPropertyPath() == null ? "参数" : item.getPropertyPath().toString();
                        String field = simplifyPath(path);
                        String detail = item.getMessage() == null ? "参数不合法" : item.getMessage();
                        return field + ": " + detail;
                    })
                    .collect(Collectors.toSet());
            if (!messages.isEmpty()) {
                return "请求参数校验失败: " + String.join("; ", messages);
            }
        }
        return "请求参数校验失败";
    }

    private String joinBindingErrors(BindingResult bindingResult) {
        List<String> details = new ArrayList<>();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            String field = fieldError.getField();
            String detail = fieldError.getDefaultMessage() == null ? "参数不合法" : fieldError.getDefaultMessage();
            details.add(field + ": " + detail);
        }
        bindingResult.getGlobalErrors().forEach(error ->
                details.add((error.getObjectName() == null ? "参数" : error.getObjectName()) + ": "
                        + (error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage()))
        );
        if (details.isEmpty()) {
            return "请求参数校验失败";
        }
        return "请求参数校验失败: " + String.join("; ", details);
    }

    private String simplifyPath(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return "参数";
        }
        String[] segments = rawPath.split("\\.");
        return segments[segments.length - 1];
    }
}


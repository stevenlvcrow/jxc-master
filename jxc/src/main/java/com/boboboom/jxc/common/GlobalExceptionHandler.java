package com.boboboom.jxc.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.boboboom.jxc.identity.application.auth.OrgScopeAccessDeniedException;
import com.boboboom.jxc.identity.application.auth.UnauthorizedException;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;

import jakarta.validation.ConstraintViolationException;

/** 全局异常处理器，统一转换业务异常、认证异常和系统异常响应。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String INIT_DATA_MESSAGE = "数据初始化未完成，请联系管理员";

    /** 处理业务异常。 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CodeDataResponse<Void>> handleBusinessException(BusinessException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** 处理机构作用域失效异常。 */
    @ExceptionHandler(OrgScopeAccessDeniedException.class)
    public ResponseEntity<CodeDataResponse<Void>> handleOrgScopeAccessDeniedException(OrgScopeAccessDeniedException ex) {
        return error(HttpStatus.FORBIDDEN, OrgScopeAccessDeniedException.CODE, ex.getMessage());
    }

    /** 处理未认证或认证失效异常。 */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<CodeDataResponse<Void>> handleUnauthorizedException(UnauthorizedException ex) {
        return error(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** 处理参数校验异常。 */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        BindException.class,
        ConstraintViolationException.class
    })
    public ResponseEntity<CodeDataResponse<Void>> handleValidationException(Exception ex) {
        String message = buildValidationMessage(ex);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    /** 处理未预期系统异常。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CodeDataResponse<Void>> handleException(Exception ex) {
        LOG.error("系统内部异常", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "系统繁忙，请稍后重试");
    }

    /** 处理数据库结构未初始化导致的 SQL 异常。 */
    @ExceptionHandler(BadSqlGrammarException.class)
    public ResponseEntity<CodeDataResponse<Void>> handleBadSqlGrammar(BadSqlGrammarException ex) {
        Throwable root = ex.getRootCause();
        String rootMessage = root == null ? ex.getMessage() : root.getMessage();
        LOG.error("数据库SQL语法异常", ex);
        if (isPostgresSqlException(root) && rootMessage != null && rootMessage.contains("does not exist")) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, INIT_DATA_MESSAGE);
        }
        if (rootMessage != null && rootMessage.contains("不存在")) {
            return error(HttpStatus.INTERNAL_SERVER_ERROR, INIT_DATA_MESSAGE);
        }
        return error(HttpStatus.INTERNAL_SERVER_ERROR, INIT_DATA_MESSAGE);
    }

    private ResponseEntity<CodeDataResponse<Void>> error(HttpStatus status, String message) {
        return error(status, status.value(), message);
    }

    private ResponseEntity<CodeDataResponse<Void>> error(HttpStatus status, int code, String message) {
        return ResponseEntity.status(status)
                .body(new CodeDataResponse<>(code, message, null));
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

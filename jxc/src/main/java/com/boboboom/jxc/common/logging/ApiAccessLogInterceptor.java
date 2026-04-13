package com.boboboom.jxc.common.logging;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.Set;

@Component
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiAccessLogInterceptor.class);
    private static final String START_TIME_ATTR = ApiAccessLogInterceptor.class.getName() + ".START_TIME";
    private static final int MAX_LOG_LENGTH = 4000;
    private static final Set<String> SENSITIVE_KEYS = new HashSet<>(Arrays.asList(
            "password", "pass", "pwd", "token", "accessToken", "refreshToken", "authorization"
    ));

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        long startTime = resolveStartTime(request);
        long costMs = System.currentTimeMillis() - startTime;
        String apiName = resolveApiName(request, handler);
        String input = truncate(maskSensitive(readRequestContent(request)));
        String output = truncate(maskSensitive(readResponseContent(response)));

        if (ex != null) {
            log.info("API [{}] input={} output={} status={} cost={}ms error={}",
                    apiName, input, output, response.getStatus(), costMs, ex.getMessage());
            return;
        }
        log.info("API [{}] input={} output={} status={} cost={}ms",
                apiName, input, output, response.getStatus(), costMs);
    }

    private long resolveStartTime(HttpServletRequest request) {
        Object value = request.getAttribute(START_TIME_ATTR);
        if (value instanceof Long longValue) {
            return longValue;
        }
        return System.currentTimeMillis();
    }

    private String resolveApiName(HttpServletRequest request, Object handler) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        if (handler instanceof HandlerMethod handlerMethod) {
            return method + " " + uri + " -> "
                    + handlerMethod.getBeanType().getSimpleName() + "#" + handlerMethod.getMethod().getName();
        }
        return method + " " + uri;
    }

    private String readRequestContent(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper wrapper) {
            byte[] body = wrapper.getContentAsByteArray();
            if (body.length > 0) {
                return convertBodyToString(body, request.getCharacterEncoding(), request.getContentType(), "request");
            }
        }
        if (request.getParameterMap().isEmpty()) {
            return "{}";
        }
        return toJson(request.getParameterMap());
    }

    private String readResponseContent(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper wrapper) {
            byte[] body = wrapper.getContentAsByteArray();
            if (body.length > 0) {
                return convertBodyToString(body, response.getCharacterEncoding(), response.getContentType(), "response");
            }
        }
        return "{}";
    }

    private String convertBodyToString(byte[] body, String charsetName, String contentType, String direction) {
        if (!isReadableContentType(contentType)) {
            return "<" + direction + " body omitted: contentType=" + contentType + ">";
        }
        try {
            if (charsetName != null) {
                return new String(body, charsetName);
            }
        } catch (Exception ignored) {
            // Fallback to UTF-8 for unexpected encoding names.
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private boolean isReadableContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        return lower.contains("application/json")
                || lower.contains("application/xml")
                || lower.contains("text/")
                || lower.contains("application/x-www-form-urlencoded");
    }

    private String maskSensitive(String content) {
        if (content == null || content.isBlank() || "{}".equals(content)) {
            return content;
        }
        String masked = content;
        for (String key : SENSITIVE_KEYS) {
            // JSON-like: "password":"xxx"
            masked = Pattern.compile("(\\\"" + Pattern.quote(key) + "\\\"\\s*:\\s*\\\")(.*?)(\\\")",
                            Pattern.CASE_INSENSITIVE)
                    .matcher(masked)
                    .replaceAll("$1***$3");
            // Query/form-like: password=xxx
            masked = Pattern.compile("(" + Pattern.quote(key) + "\\s*=\\s*)([^&\\s,}\\]]+)",
                            Pattern.CASE_INSENSITIVE)
                    .matcher(masked)
                    .replaceAll("$1***");
        }
        return masked;
    }

    private boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.contains(key) || SENSITIVE_KEYS.contains(lower);
    }

    private String toJson(Map<String, String[]> params) {
        StringBuilder sb = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String[]> entry : params.entrySet()) {
            String key = entry.getKey();
            String[] value = entry.getValue();
            if (index > 0) {
                sb.append(',');
            }
            sb.append('\"').append(escapeJson(key)).append("\":");
            if (isSensitiveKey(key)) {
                sb.append("\"***\"");
                index++;
                continue;
            }
            if (value == null) {
                sb.append("null");
                index++;
                continue;
            }
            if (value.length == 1) {
                sb.append('\"').append(escapeJson(value[0])).append('\"');
                index++;
                continue;
            }
            sb.append('[');
            for (int i = 0; i < value.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append('\"').append(escapeJson(value[i])).append('\"');
            }
            sb.append(']');
            index++;
        }
        sb.append('}');
        return sb.toString();
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String truncate(String content) {
        if (content == null) {
            return "null";
        }
        if (content.length() <= MAX_LOG_LENGTH) {
            return content;
        }
        return content.substring(0, MAX_LOG_LENGTH) + "...(truncated)";
    }
}

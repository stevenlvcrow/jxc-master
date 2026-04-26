package com.boboboom.jxc.common;

/** 统一接口响应模型，封装成功状态、消息和数据。 */
public record ApiResponse<T>(boolean success, String message, T data) {

    /** 构造成功响应。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data);
    }

    /** 构造成功响应。 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, "OK", null);
    }

    /** 构造失败响应。 */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, message, null);
    }
}


package com.boboboom.jxc.identity.interfaces.rest.response;

public class CodeDataResponse<T> {

    private int code;
    private String message;
    private T data;

    public CodeDataResponse() {
    }

    public CodeDataResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> CodeDataResponse<T> ok(T data) {
        return new CodeDataResponse<>(0, "ok", data);
    }

    public static CodeDataResponse<Void> ok() {
        return new CodeDataResponse<>(0, "ok", null);
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
}

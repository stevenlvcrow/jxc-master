package com.boboboom.jxc.identity.interfaces.rest.response;

/** 身份与权限响应模型，承载接口返回数据。 */
public class CodeDataResponse<T> {

    private int code;
    private String message;
    private T data;

    /** 身份与权限响应模型，承载接口返回数据。 */
    public CodeDataResponse() {
    }

    /** 身份与权限响应模型，承载接口返回数据。 */
    public CodeDataResponse(int codeValue, String messageValue, T dataValue) {
        this.code = codeValue;
        this.message = messageValue;
        this.data = dataValue;
    }

    /** 构造成功响应。 */
    public static <T> CodeDataResponse<T> ok(T data) {
        return new CodeDataResponse<>(0, "ok", data);
    }

    /** 构造成功响应。 */
    public static CodeDataResponse<Void> ok() {
        return new CodeDataResponse<>(0, "ok", null);
    }

    /** 获取Code。 */
    public int getCode() {
        return code;
    }

    /** 设置Code。 */
    public void setCode(int codeValue) {
        this.code = codeValue;
    }

    /** 获取Message。 */
    public String getMessage() {
        return message;
    }

    /** 设置Message。 */
    public void setMessage(String messageValue) {
        this.message = messageValue;
    }

    /** 获取Data。 */
    public T getData() {
        return data;
    }

    /** 设置Data。 */
    public void setData(T dataValue) {
        this.data = dataValue;
    }
}

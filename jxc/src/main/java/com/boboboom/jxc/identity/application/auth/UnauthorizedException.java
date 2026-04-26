package com.boboboom.jxc.identity.application.auth;

/** 身份与权限异常类型，表示业务或系统处理失败。 */
public class UnauthorizedException extends RuntimeException {

    /** 身份与权限异常类型，表示业务或系统处理失败。 */
    public UnauthorizedException(String message) {
        super(message);
    }
}

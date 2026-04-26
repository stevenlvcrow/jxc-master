package com.boboboom.jxc.common;

/** 业务异常，表示可向前端返回的业务规则失败。 */
public class BusinessException extends RuntimeException {

    /** 业务异常，表示可向前端返回的业务规则失败。 */
    public BusinessException(String message) {
        super(message);
    }
}


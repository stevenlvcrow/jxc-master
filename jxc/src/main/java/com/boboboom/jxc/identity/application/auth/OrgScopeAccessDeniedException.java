package com.boboboom.jxc.identity.application.auth;

/** 机构作用域失效异常，表示当前登录账号不能继续使用已选机构。 */
public class OrgScopeAccessDeniedException extends RuntimeException {

    public static final int CODE = 40301;

    /** 机构作用域失效异常，表示当前登录账号不能继续使用已选机构。 */
    public OrgScopeAccessDeniedException(String message) {
        super(message);
    }
}

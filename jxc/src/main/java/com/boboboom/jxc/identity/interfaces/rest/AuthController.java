package com.boboboom.jxc.identity.interfaces.rest;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boboboom.jxc.identity.application.service.AuthApplicationService;
import com.boboboom.jxc.identity.application.service.AuthApplicationService.CurrentUserRoleResult;
import com.boboboom.jxc.identity.interfaces.rest.request.AuthLoginRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserAccountChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserPasswordChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.CurrentUserPhoneChangeRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RefreshTokenRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthLoginResult;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthRefreshResult;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.CurrentUserResult;

import jakarta.validation.Valid;

/** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
@Validated
@RestController
@RequestMapping({"/auth", "/api/identity/auth"})
public class AuthController {

    private final AuthApplicationService authApplicationService;

    /** 身份与权限接口入口，负责接收请求、调用业务服务并返回统一响应。 */
    public AuthController(AuthApplicationService authApplicationServiceValue) {
        this.authApplicationService = authApplicationServiceValue;
    }

    /** 处理PostMapping。 */
    @PostMapping("/login")
    public CodeDataResponse<AuthLoginResult> login(@Valid @RequestBody AuthLoginRequest request) {
        return CodeDataResponse.ok(authApplicationService.login(request));
    }

    /** 处理PostMapping。 */
    @PostMapping("/refresh")
    public CodeDataResponse<AuthRefreshResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return CodeDataResponse.ok(authApplicationService.refresh(request));
    }

    /** 处理GetMapping。 */
    @GetMapping("/me")
    public CodeDataResponse<CurrentUserResult> me() {
        return CodeDataResponse.ok(authApplicationService.me());
    }

    /** 处理PutMapping。 */
    @PutMapping("/me/password")
    public CodeDataResponse<Void> changeCurrentPassword(@Valid @RequestBody CurrentUserPasswordChangeRequest request) {
        authApplicationService.changeCurrentPassword(request);
        return CodeDataResponse.ok();
    }

    /** 处理PutMapping。 */
    @PutMapping("/me/phone")
    public CodeDataResponse<CurrentUserResult> changeCurrentPhone(@Valid @RequestBody CurrentUserPhoneChangeRequest request) {
        return CodeDataResponse.ok(authApplicationService.changeCurrentPhone(request));
    }

    /** 处理PutMapping。 */
    @PutMapping("/me/account")
    public CodeDataResponse<CurrentUserResult> changeCurrentAccount(@Valid @RequestBody CurrentUserAccountChangeRequest request) {
        return CodeDataResponse.ok(authApplicationService.changeCurrentAccount(request));
    }

    /** 处理GetMapping。 */
    @GetMapping("/me/roles")
    public CodeDataResponse<List<CurrentUserRoleResult>> meRoles(@RequestParam(required = false) String orgId) {
        return CodeDataResponse.ok(authApplicationService.meRoles(orgId));
    }

    /** 处理PostMapping。 */
    @PostMapping("/logout")
    public CodeDataResponse<Void> logout() {
        authApplicationService.logout();
        return CodeDataResponse.ok();
    }
}

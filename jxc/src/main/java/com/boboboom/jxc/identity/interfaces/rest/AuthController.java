package com.boboboom.jxc.identity.interfaces.rest;

import com.boboboom.jxc.identity.application.service.AuthApplicationService;
import com.boboboom.jxc.identity.application.service.AuthApplicationService.CurrentUserRoleResult;
import com.boboboom.jxc.identity.interfaces.rest.request.AuthLoginRequest;
import com.boboboom.jxc.identity.interfaces.rest.request.RefreshTokenRequest;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthLoginResult;
import com.boboboom.jxc.identity.interfaces.rest.response.AuthRefreshResult;
import com.boboboom.jxc.identity.interfaces.rest.response.CodeDataResponse;
import com.boboboom.jxc.identity.interfaces.rest.response.CurrentUserResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping({"/auth", "/api/identity/auth"})
public class AuthController {

    private final AuthApplicationService authApplicationService;

    public AuthController(AuthApplicationService authApplicationService) {
        this.authApplicationService = authApplicationService;
    }

    @PostMapping("/login")
    public CodeDataResponse<AuthLoginResult> login(@Valid @RequestBody AuthLoginRequest request) {
        return authApplicationService.login(request);
    }

    @PostMapping("/refresh")
    public CodeDataResponse<AuthRefreshResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authApplicationService.refresh(request);
    }

    @GetMapping("/me")
    public CodeDataResponse<CurrentUserResult> me() {
        return authApplicationService.me();
    }

    @GetMapping("/me/roles")
    public CodeDataResponse<List<CurrentUserRoleResult>> meRoles(@RequestParam(required = false) String orgId) {
        return authApplicationService.meRoles(orgId);
    }

    @PostMapping("/logout")
    public CodeDataResponse<Void> logout() {
        return authApplicationService.logout();
    }
}

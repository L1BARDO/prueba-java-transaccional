package com.switchtx.infrastructure.adapter.in.rest.auth;

import com.switchtx.application.port.in.auth.AuthenticatedUser;
import com.switchtx.application.port.in.auth.CurrentUserUseCase;
import com.switchtx.application.port.in.auth.LoginCommand;
import com.switchtx.application.port.in.auth.LoginUseCase;
import com.switchtx.infrastructure.security.JwtTokenProvider;
import com.switchtx.infrastructure.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final LoginUseCase loginUseCase;
    private final CurrentUserUseCase currentUserUseCase;
    private final JwtTokenProvider tokenProvider;

    @Override
    public LoginResponse login(LoginRequest request) {
        AuthenticatedUser user = loginUseCase.authenticate(new LoginCommand(request.username(), request.password()));
        String token = tokenProvider.generateToken(user);
        return LoginResponse.of(token, tokenProvider.getExpirationSeconds(), user);
    }

    @Override
    public CurrentUserResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Usuario no autenticado");
        }
        return CurrentUserResponse.from(principal);
    }
}

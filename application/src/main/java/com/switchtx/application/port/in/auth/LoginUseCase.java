package com.switchtx.application.port.in.auth;

public interface LoginUseCase {

    AuthenticatedUser authenticate(LoginCommand command);
}

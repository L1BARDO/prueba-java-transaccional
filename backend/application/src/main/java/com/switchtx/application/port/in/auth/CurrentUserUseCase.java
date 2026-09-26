package com.switchtx.application.port.in.auth;

public interface CurrentUserUseCase {

    AuthenticatedUser getCurrentUser(String username);
}

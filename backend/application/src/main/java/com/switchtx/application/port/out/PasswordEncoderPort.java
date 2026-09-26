package com.switchtx.application.port.out;

public interface PasswordEncoderPort {

    boolean matches(CharSequence rawPassword, String encodedPassword);

    String encode(CharSequence rawPassword);
}

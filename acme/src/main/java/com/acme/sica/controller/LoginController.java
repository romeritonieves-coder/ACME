package com.acme.sica.controller;

import com.acme.sica.config.SicaApplicationContext;
import com.acme.sica.dto.LoginRequest;
import com.acme.sica.dto.LoginResponse;

public final class LoginController {
    private final SicaApplicationContext context;

    public LoginController(SicaApplicationContext context) {
        this.context = context;
    }

    public LoginResponse autenticar(String email, char[] password) {
        return context.authService().autenticar(
                LoginRequest.builder().email(email).password(new String(password)).build(), "desktop");
    }
}

package com.paper.mes.auth.service;

import com.paper.mes.auth.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    public static final String COOKIE_NAME = "paper_mes_session";
    private final AuthProperties properties;

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public void write(HttpServletResponse response, String token) {
        add(response, token, Duration.ofHours(properties.getSessionHours()));
    }

    public void clear(HttpServletResponse response) {
        add(response, "", Duration.ZERO);
    }

    private void add(HttpServletResponse response, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.isCookieSecure())
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}

package com.paper.mes.auth.service;

import com.paper.mes.auth.config.AuthProperties;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieServiceTest {

    @Test
    void write_createsProtectedSessionCookie() {
        AuthProperties properties = new AuthProperties();
        properties.setCookieSecure(true);
        AuthCookieService service = new AuthCookieService(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.write(response, "secret-token");

        String header = response.getHeader("Set-Cookie");
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("Secure"));
        assertTrue(header.contains("SameSite=Strict"));
    }

    @Test
    void read_returnsSessionCookieValue() {
        AuthCookieService service = new AuthCookieService(new AuthProperties());
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(AuthCookieService.COOKIE_NAME, "session-token"));

        assertEquals("session-token", service.read(request));
    }
}

package com.paper.mes.auth.config;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthInterceptorTest {

    private final AuthService authService = mock(AuthService.class);
    private final AuthInterceptor interceptor = new AuthInterceptor(authService);

    @AfterEach
    void tearDown() {
        AuthContextHolder.clear();
    }

    @Test
    void preHandle_cookieWriteWithoutRequestHeader_rejectsRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        when(authService.isCookieAuthentication(request)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void preHandle_cookieWriteWithRequestHeader_authenticatesRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        when(authService.isCookieAuthentication(request)).thenReturn(true);
        when(authService.resolveToken(request)).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder().uuid("user").build());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void afterConcurrentHandlingStarted_clearsThreadLocalUser() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user").build());

        interceptor.afterConcurrentHandlingStarted(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertNull(AuthContextHolder.getCurrentUser());
    }
}

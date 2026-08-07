package com.paper.mes.auth.config;

import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.service.AuthService;
import com.paper.mes.auth.permission.PublicEndpoint;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void preHandle_cookieWriteWithRequestHeader_authenticatesRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/orders");
        request.addHeader("X-Requested-With", "XMLHttpRequest");
        when(authService.isCookieAuthentication(request)).thenReturn(true);
        when(authService.resolveToken(request)).thenReturn("token");
        when(authService.currentUser("token")).thenReturn(CurrentUser.builder().uuid("user").build());

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    @Test
    void preHandle_withoutToken_writesJsonUnauthorizedResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("GET", "/api/orders"), response, new Object());

        assertFalse(allowed);
        assertEquals(ResultCode.UNAUTHORIZED, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("请先登录"));
    }

    @Test
    void preHandle_publicEndpoint_allowsAnonymousRequest() throws Exception {
        HandlerMethod handler = new HandlerMethod(new PublicHandler(), "login");

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/api/auth/login"),
                new MockHttpServletResponse(), handler);

        assertTrue(allowed);
    }

    @Test
    void afterConcurrentHandlingStarted_clearsThreadLocalUser() {
        AuthContextHolder.setCurrentUser(CurrentUser.builder().uuid("user").build());

        interceptor.afterConcurrentHandlingStarted(
                new MockHttpServletRequest(), new MockHttpServletResponse(), new Object());

        assertNull(AuthContextHolder.getCurrentUser());
    }

    private static final class PublicHandler {
        @PublicEndpoint
        public void login() {
        }
    }
}

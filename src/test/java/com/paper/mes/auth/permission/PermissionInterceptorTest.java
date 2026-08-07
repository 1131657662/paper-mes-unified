package com.paper.mes.auth.permission;

import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PermissionInterceptorTest {

    private final PermissionInterceptor interceptor = new PermissionInterceptor(mock(PermissionChecker.class));

    @Test
    void preHandle_withoutExplicitPolicy_deniesEndpoint() throws Exception {
        HandlerMethod handler = new HandlerMethod(new PolicyHandler(), "missingPolicy");

        assertThatThrownBy(() -> preHandle(handler))
                .isInstanceOf(BusinessException.class)
                .hasMessage("接口权限策略未配置");
    }

    @Test
    void preHandle_authenticatedEndpoint_allowsAuthenticatedRequest() throws Exception {
        HandlerMethod handler = new HandlerMethod(new PolicyHandler(), "authenticated");

        assertThat(preHandle(handler)).isTrue();
    }

    private boolean preHandle(HandlerMethod handler) {
        return interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handler);
    }

    private static final class PolicyHandler {
        public void missingPolicy() {
        }

        @AuthenticatedEndpoint
        public void authenticated() {
        }
    }
}

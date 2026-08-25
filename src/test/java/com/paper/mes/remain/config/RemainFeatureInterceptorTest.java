package com.paper.mes.remain.config;

import com.paper.mes.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class RemainFeatureInterceptorTest {

    private final HttpServletRequest request = mock(HttpServletRequest.class);

    @Test
    void rejectsRemainRequestsWhileFeatureIsDisabled() {
        RemainProperties properties = new RemainProperties();
        RemainFeatureInterceptor interceptor = new RemainFeatureInterceptor(properties);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> interceptor.preHandle(request, null, new Object()));

        assertEquals(503, exception.getCode());
        assertEquals("REMAIN_MODULE_DISABLED", exception.getErrorCode());
    }

    @Test
    void allowsRemainRequestsWhenFeatureIsEnabled() {
        RemainProperties properties = new RemainProperties();
        properties.setEnabled(true);
        RemainFeatureInterceptor interceptor = new RemainFeatureInterceptor(properties);

        assertDoesNotThrow(() -> interceptor.preHandle(request, null, new Object()));
    }
}

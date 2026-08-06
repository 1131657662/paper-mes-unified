package com.paper.mes.common;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void acceptedRequestIdIsAvailableToErrorResponsesAndReturnedAsHeader() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "browser-request-123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<R<Void>> body = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                body.set(R.fail(ResultCode.BAD_REQUEST, "invalid")));

        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("browser-request-123");
        assertThat(body.get().getRequestId()).isEqualTo("browser-request-123");
        assertThat(MDC.get(RequestIdContext.MDC_KEY)).isNull();
    }

    @Test
    void unsafeRequestIdIsReplaced() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "bad value with spaces");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) -> { });

        assertThat(response.getHeader(RequestIdFilter.HEADER))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}

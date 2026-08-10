package com.paper.mes.observability;

import com.paper.mes.observability.web.RumRequestSizeFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RumRequestSizeFilterTest {

    private final RumRequestSizeFilter filter = new RumRequestSizeFilter();

    @Test
    void doFilter_rejectsOversizedTelemetryBeforeControllerBinding() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/rum");
        request.setContent(new byte[4097]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(chain.getRequest()).isNull();
    }
}

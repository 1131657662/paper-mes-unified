package com.paper.mes.common;

import org.slf4j.MDC;

/** Provides the request identifier already validated by {@link RequestIdFilter}. */
public final class RequestIdContext {

    static final String MDC_KEY = "requestId";

    private RequestIdContext() {
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}

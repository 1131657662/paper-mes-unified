package com.paper.mes.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.R;
import com.paper.mes.common.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class AuthenticationFailureResponseWriter {

    private static final ObjectMapper JSON = new ObjectMapper();

    private AuthenticationFailureResponseWriter() {
    }

    static boolean writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(ResultCode.UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JSON.writeValue(response.getOutputStream(), R.fail(ResultCode.UNAUTHORIZED, message));
        return false;
    }
}

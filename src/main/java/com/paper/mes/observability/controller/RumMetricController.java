package com.paper.mes.observability.controller;

import com.paper.mes.auth.permission.PublicEndpoint;
import com.paper.mes.observability.dto.RumMetricRequest;
import com.paper.mes.observability.service.RumService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rum")
@RequiredArgsConstructor
public class RumMetricController {

    private final RumService rumService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PublicEndpoint
    public ResponseEntity<Void> collect(@Valid @RequestBody RumMetricRequest event, HttpServletRequest request) {
        rumService.record(event, request.getRemoteAddr());
        return ResponseEntity.noContent().build();
    }
}

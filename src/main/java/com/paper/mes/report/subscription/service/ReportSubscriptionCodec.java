package com.paper.mes.report.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.report.dto.ReportQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportSubscriptionCodec {

    private final ObjectMapper objectMapper;

    public String write(ReportQuery query) {
        try {
            return objectMapper.writeValueAsString(query);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("报表订阅筛选条件无法保存");
        }
    }

    public ReportQuery read(String value) {
        try {
            return objectMapper.readValue(value, ReportQuery.class);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("报表订阅筛选条件已损坏");
        }
    }
}

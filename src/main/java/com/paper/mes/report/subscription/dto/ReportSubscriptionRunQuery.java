package com.paper.mes.report.subscription.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class ReportSubscriptionRunQuery {
    @Min(1)
    private Integer current = 1;
    @Min(1)
    @Max(50)
    private Integer size = 10;
    @Min(1)
    @Max(4)
    private Integer runStatus;
}

package com.paper.mes.report.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class ReportDetailsVO {

    List<ReportDetailVO> rows;
    long total;
    int displayLimit;
    boolean truncated;
}

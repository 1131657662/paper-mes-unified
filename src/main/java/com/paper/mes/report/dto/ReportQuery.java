package com.paper.mes.report.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 统计报表通用查询条件，按加工单制单日期过滤。
 */
@Data
public class ReportQuery {

    private LocalDate dateFrom;
    private LocalDate dateTo;
    private String customerUuid;
    private String paperName;
    private Integer mainStepType;
    private Integer processMode;
    private String machineUuid;
    private Integer settleType;
    private Integer isInvoice;
    private Integer orderStatus;
    private String dimension;
}

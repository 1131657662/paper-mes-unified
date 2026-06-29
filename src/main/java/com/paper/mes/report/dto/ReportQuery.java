package com.paper.mes.report.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 报表通用查询入参：按制单日期范围与可选客户过滤。聚合结果不分页。
 */
@Data
public class ReportQuery {

    /** 制单日期起（含），按 order_date 过滤 */
    private LocalDate dateFrom;

    /** 制单日期止（含） */
    private LocalDate dateTo;

    /** 可选：限定单个客户 */
    private String customerUuid;
}

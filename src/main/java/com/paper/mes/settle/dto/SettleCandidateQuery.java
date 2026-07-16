package com.paper.mes.settle.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 可结算加工单查询。账期按回录完成日期归属，历史数据缺失时回退到制单日期。
 */
@Data
public class SettleCandidateQuery {

    private String keyword;
    private String customerUuid;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private long current = 1;
    private long size = 20;
}

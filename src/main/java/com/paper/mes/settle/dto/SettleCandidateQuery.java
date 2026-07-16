package com.paper.mes.settle.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 可结算加工单查询。当前结算模型按整张加工单入账，候选仅包含已完成且未结算的加工单。
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

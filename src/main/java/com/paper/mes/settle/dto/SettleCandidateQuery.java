package com.paper.mes.settle.dto;

import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 可结算加工单查询。账期按回录完成日期归属，历史数据缺失时回退到制单日期。
 */
@Data
public class SettleCandidateQuery {

    @Size(max = 100, message = "关键字不能超过100个字符")
    private String keyword;
    @Size(max = 100, message = "单次最多指定100张加工单")
    private List<@Size(max = 64, message = "加工单标识不能超过64个字符") String> orderUuids;
    private String customerUuid;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    @Min(value = 1, message = "页码必须大于0")
    private long current = 1;
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private long size = 20;

    @AssertTrue(message = "开始日期不能晚于结束日期")
    public boolean isPeriodOrdered() {
        return periodStart == null || periodEnd == null || !periodStart.isAfter(periodEnd);
    }
}

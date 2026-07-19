package com.paper.mes.settle.dto;

import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 结算单列表查询入参。
 */
@Data
public class SettleQuery {

    /** 关键字：命中结算单号或客户名 */
    @Size(max = 100, message = "关键字不能超过100个字符")
    private String keyword;
    private String customerUuid;
    /** 1待收款 2部分收款 3全部结清 4已作废 */
    @Min(value = 1, message = "结算状态不正确")
    @Max(value = 4, message = "结算状态不正确")
    private Integer settleStatus;
    /** 1按单 2按月批量 */
    @Min(value = 1, message = "结算类型不正确")
    @Max(value = 3, message = "结算类型不正确")
    private Integer settleType;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    /** today今日待收 overdue逾期 upcoming后续到期 reminded今日已提醒 */
    @Pattern(regexp = "today|overdue|upcoming|reminded", message = "催收队列类型不正确")
    private String collectionQueue;

    @Min(value = 1, message = "页码必须大于0")
    private long current = 1;
    @Min(value = 1, message = "每页条数必须大于0")
    @Max(value = 100, message = "每页条数不能超过100")
    private long size = 10;

    @AssertTrue(message = "开始日期不能晚于结束日期")
    public boolean isDateRangeOrdered() {
        return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
    }
}

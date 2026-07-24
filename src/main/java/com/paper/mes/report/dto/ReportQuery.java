package com.paper.mes.report.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 统计报表通用查询条件，按加工单归属日期过滤。
 */
@Data
public class ReportQuery {

    @Pattern(regexp = "^[0-9a-fA-F-]{32,36}$", message = "指标发布包标识格式不正确")
    private String metricReleaseUuid;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    @Size(max = 64, message = "客户标识不能超过64个字符")
    private String customerUuid;
    @Size(max = 100, message = "品名不能超过100个字符")
    private String paperName;
    @Min(value = 1, message = "主工艺类型无效")
    @Max(value = 2, message = "主工艺类型无效")
    private Integer mainStepType;
    @Min(value = 1, message = "加工方式无效")
    @Max(value = 3, message = "加工方式无效")
    private Integer processMode;
    @Size(max = 64, message = "机台标识不能超过64个字符")
    private String machineUuid;
    @Min(value = 1, message = "结算方式无效")
    @Max(value = 2, message = "结算方式无效")
    private Integer settleType;
    @Min(value = 1, message = "开票状态无效")
    @Max(value = 2, message = "开票状态无效")
    private Integer isInvoice;
    @Min(value = 0, message = "加工单状态无效")
    @Max(value = 6, message = "加工单状态无效")
    private Integer orderStatus;
    @Pattern(regexp = "month|customer|paper|process|machine|invoice|settleType|status",
            message = "统计维度无效")
    private String dimension;

    @JsonIgnore
    @AssertTrue(message = "开始日期不能晚于结束日期")
    public boolean isDateRangeValid() {
        return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
    }
}

package com.paper.mes.report.subscription.dto;

import com.paper.mes.report.dto.ReportQuery;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
public class ReportSubscriptionSaveDTO {
    @NotBlank(message = "订阅名称不能为空")
    @Size(max = 100, message = "订阅名称不能超过100个字符")
    private String subscriptionName;
    @NotBlank(message = "来源报表页面不能为空")
    @Size(max = 160, message = "来源报表页面不能超过160个字符")
    @jakarta.validation.constraints.Pattern(
            regexp = "^/reports/(overview|production|quality-loss|settlement|collection|inventory|delivery|explorer)$",
            message = "来源报表页面无效")
    private String reportPath = "/reports/overview";
    @NotNull(message = "执行周期不能为空")
    @Min(value = 1, message = "执行周期无效")
    @Max(value = 3, message = "执行周期无效")
    private Integer scheduleType;
    @NotNull(message = "执行时间不能为空")
    private LocalTime executionTime;
    @Min(value = 1, message = "星期值无效")
    @Max(value = 7, message = "星期值无效")
    private Integer weekDay;
    @Min(value = 1, message = "每月执行日无效")
    @Max(value = 28, message = "每月执行日无效")
    private Integer monthDay;
    @NotBlank(message = "时区不能为空")
    @Size(max = 40, message = "时区不能超过40个字符")
    private String timezone = "Asia/Shanghai";
    @Valid
    @NotNull(message = "报表筛选条件不能为空")
    private ReportQuery reportQuery;
    @NotNull(message = "指标发布策略不能为空")
    @Min(value = 1, message = "指标发布策略无效")
    @Max(value = 2, message = "指标发布策略无效")
    private Integer releasePolicy = 1;
    @Size(max = 36, message = "固定发布包标识过长")
    private String pinnedReleaseUuid;
    @NotNull(message = "统计周期策略不能为空")
    @Min(value = 1, message = "统计周期策略无效")
    @Max(value = 4, message = "统计周期策略无效")
    private Integer periodPolicy;
    @NotNull(message = "启用状态不能为空")
    @Min(value = 0, message = "启用状态无效")
    @Max(value = 1, message = "启用状态无效")
    private Integer isEnabled = 1;
    @Size(max = 50, message = "接收人不能超过50人")
    private Set<@NotBlank(message = "接收人标识不能为空") @Size(max = 36, message = "接收人标识过长") String>
            recipientUuids = new LinkedHashSet<>();
    @Min(value = 1, message = "数据版本无效")
    private Integer version;

    @AssertTrue(message = "执行周期参数不完整")
    public boolean isScheduleValid() {
        if (scheduleType == null) return true;
        if (scheduleType == 1) return weekDay == null && monthDay == null;
        if (scheduleType == 2) return weekDay != null && monthDay == null;
        return scheduleType != 3 || weekDay == null && monthDay != null;
    }

    @AssertTrue(message = "时区无效")
    public boolean isTimezoneValid() {
        if (timezone == null || timezone.isBlank()) return true;
        try {
            ZoneId.of(timezone);
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @AssertTrue(message = "固定统计区间必须同时指定开始和结束日期")
    public boolean isFixedPeriodValid() {
        return periodPolicy == null || periodPolicy != 4 || reportQuery == null
                || reportQuery.getDateFrom() != null && reportQuery.getDateTo() != null;
    }

    @AssertTrue(message = "固定发布包策略必须指定发布包")
    public boolean isReleasePolicyValid() {
        if (releasePolicy == null) return true;
        return releasePolicy == 1 && !hasText(pinnedReleaseUuid)
                || releasePolicy == 2 && hasText(pinnedReleaseUuid);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

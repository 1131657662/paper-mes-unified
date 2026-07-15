package com.paper.mes.oplog.dto;

import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 操作日志查询参数
 */
@Data
public class OperationLogQuery {

    /** 当前页 */
    @Min(value = 1, message = "页码不能小于1")
    @Max(value = 1000000, message = "页码超出允许范围")
    private Integer current = 1;

    /** 每页大小 */
    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer size = 20;

    /** 业务类型 */
    @Size(max = 50, message = "业务类型不能超过50个字符")
    private String bizType;

    /** 业务单号 */
    @Size(max = 100, message = "业务单号不能超过100个字符")
    private String bizNo;

    /** 动作类型 */
    @Size(max = 50, message = "动作类型不能超过50个字符")
    private String actionType;

    /** 操作人 */
    @Size(max = 50, message = "操作人不能超过50个字符")
    private String operator;

    /** 变更字段名 */
    @Size(max = 100, message = "字段名不能超过100个字符")
    private String fieldName;

    /** 备注关键字 */
    @Size(max = 100, message = "备注关键字不能超过100个字符")
    private String remark;

    /** 操作日期起 */
    private LocalDate dateFrom;

    /** 操作日期止 */
    private LocalDate dateTo;

    @AssertTrue(message = "开始日期不能晚于结束日期")
    public boolean isDateRangeValid() {
        return dateFrom == null || dateTo == null || !dateFrom.isAfter(dateTo);
    }
}

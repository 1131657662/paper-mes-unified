package com.paper.mes.oplog.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 操作日志查询参数
 */
@Data
public class OperationLogQuery {

    /** 当前页 */
    private Integer current = 1;

    /** 每页大小 */
    private Integer size = 20;

    /** 业务类型 */
    private String bizType;

    /** 业务单号 */
    private String bizNo;

    /** 动作类型 */
    private String actionType;

    /** 操作人 */
    private String operator;

    /** 操作日期起 */
    private LocalDate dateFrom;

    /** 操作日期止 */
    private LocalDate dateTo;
}

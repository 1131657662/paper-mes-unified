package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_remain_application")
public class RemainApplication {

    @TableId
    private String uuid;
    private String registrationUuid;
    private String settleUuid;
    private String adjustmentUuid;
    private String receiveUuid;
    private String customerUuid;
    private String applicationType;
    private String status;
    private BigDecimal amount;
    private BigDecimal weight;
    private String requestId;
    private String requestHash;
    private String reversalOfUuid;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private Integer version;
    private Integer isDeleted;
}

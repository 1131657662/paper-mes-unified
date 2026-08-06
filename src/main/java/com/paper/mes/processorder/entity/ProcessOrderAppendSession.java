package com.paper.mes.processorder.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_process_order_append_session")
public class ProcessOrderAppendSession extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String orderUuid;
    private String sessionNo;
    private Integer baseOrderVersion;
    private String status;
    private String reason;
    private String operator;
    private String commitRequestId;
    private LocalDateTime applyTime;
}

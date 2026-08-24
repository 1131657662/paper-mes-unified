package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_remain_adjustment_line")
public class RemainAdjustmentLine {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String adjustmentUuid;
    private String registrationLineUuid;
    private BigDecimal amount;
    private BigDecimal weight;
    private LocalDateTime createTime;
}

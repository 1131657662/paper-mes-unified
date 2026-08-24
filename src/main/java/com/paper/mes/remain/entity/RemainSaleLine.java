package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_remain_sale_line")
public class RemainSaleLine {
    @TableId(type = IdType.ASSIGN_UUID)
    private String uuid;
    private String saleUuid;
    private String lotUuid;
    private String registrationLineUuid;
    private BigDecimal systemWeight;
    private BigDecimal amount;
    private LocalDateTime createTime;
}

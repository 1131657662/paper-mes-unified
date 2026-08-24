package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("biz_remain_application_line")
public class RemainApplicationLine {

    @TableId
    private String uuid;
    private String applicationUuid;
    private String registrationLineUuid;
    private BigDecimal amount;
    private BigDecimal weight;
}

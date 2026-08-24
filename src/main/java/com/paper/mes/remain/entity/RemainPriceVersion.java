package com.paper.mes.remain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("biz_remain_price_version")
public class RemainPriceVersion {

    @TableId
    private String uuid;
    private String registrationUuid;
    private Integer versionNo;
    private String pricingBasis;
    private BigDecimal totalAmount;
    private String requestId;
    private String requestHash;
    private String status;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
}

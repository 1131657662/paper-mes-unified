package com.paper.mes.settle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登记一笔收款入参。
 */
@Data
public class ReceiveDTO {

    @NotNull(message = "收款金额不能为空")
    @Positive(message = "收款金额必须大于0")
    private BigDecimal receiveAmount;

    /** 1现金 2转账 3微信 4支付宝 */
    @NotNull(message = "收款方式不能为空")
    private Integer payMethod;

    private String payNo;
    private String operator;

    /** 收款时间，可空默认 now。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime receiveDate;

    private String remark;
}

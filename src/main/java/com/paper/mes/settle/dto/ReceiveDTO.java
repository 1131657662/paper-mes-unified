package com.paper.mes.settle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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
    @Min(value = 1, message = "收款方式不正确")
    @Max(value = 4, message = "收款方式不正确")
    private Integer payMethod;

    @Size(max = 80, message = "流水号不能超过80个字符")
    private String payNo;
    @Size(max = 50, message = "经办人不能超过50个字符")
    private String operator;

    /** 收款时间，可空默认 now。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime receiveDate;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

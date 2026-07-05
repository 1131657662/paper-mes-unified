package com.paper.mes.settle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登记一笔收款，可由现金实收和废纸抵扣共同组成。
 */
@Data
public class ReceiveDTO {

    /** 兼容旧接口：未传 cashAmount/scrapOffsetAmount 时按现金收款处理。 */
    @PositiveOrZero(message = "收款金额不能为负")
    private BigDecimal receiveAmount;

    /** 1现金 2转账 3微信 4支付宝；纯废纸抵扣可为空。 */
    @Min(value = 1, message = "收款方式不正确")
    @Max(value = 4, message = "收款方式不正确")
    private Integer payMethod;

    @PositiveOrZero(message = "现金实收不能为负")
    private BigDecimal cashAmount;

    @PositiveOrZero(message = "废纸抵扣金额不能为负")
    private BigDecimal scrapOffsetAmount;

    @PositiveOrZero(message = "废纸重量不能为负")
    private BigDecimal scrapWeight;

    @Size(max = 80, message = "流水号不能超过80个字符")
    private String payNo;

    @Size(max = 50, message = "经办人不能超过50个字符")
    private String operator;

    /** 收款时间，可为空，默认使用当前时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime receiveDate;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

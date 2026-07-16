package com.paper.mes.settle.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 登记一笔收款，可由实际到账、废纸抵扣和优惠核销共同组成。
 */
@Data
public class ReceiveDTO {

    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestId;

    /** 兼容旧接口：未传明细金额时按实际到账处理。 */
    @PositiveOrZero(message = "收款金额不能为负")
    private BigDecimal receiveAmount;

    /** 1现金 2转账 3微信 4支付宝；纯废纸抵扣可为空。 */
    @Min(value = 1, message = "收款方式不正确")
    @Max(value = 4, message = "收款方式不正确")
    private Integer payMethod;

    @PositiveOrZero(message = "实际到账不能为负")
    private BigDecimal cashAmount;

    @PositiveOrZero(message = "废纸抵扣金额不能为负")
    private BigDecimal scrapOffsetAmount;

    @PositiveOrZero(message = "优惠金额不能为负")
    private BigDecimal discountAmount;

    @Size(max = 255, message = "优惠原因不能超过255个字符")
    private String discountReason;

    @Size(max = 36, message = "优惠审批编号不能超过36个字符")
    private String discountApprovalUuid;

    @PositiveOrZero(message = "废纸重量不能为负")
    private BigDecimal scrapWeight;

    @Size(max = 80, message = "流水号不能超过80个字符")
    private String payNo;

    /** 收款时间，可为空，默认使用当前时间。 */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @PastOrPresent(message = "收款时间不能晚于当前时间")
    private LocalDateTime receiveDate;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库确认入参（签收信息）。
 */
@Data
public class DeliveryConfirmDTO {

    @Size(max = 50, message = "签收人不能超过50个字符")
    private String signUser;
    /** 签收时间，留空默认当前时间。 */
    @PastOrPresent(message = "签收时间不能晚于当前时间")
    private LocalDateTime signTime;
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
    /** 最终签收时存在现结风险，由当前操作人明确确认后传 true。 */
    private boolean forceRelease;
}

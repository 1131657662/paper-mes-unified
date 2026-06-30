package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.Size;
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
    private LocalDateTime signTime;
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

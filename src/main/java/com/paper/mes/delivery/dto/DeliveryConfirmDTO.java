package com.paper.mes.delivery.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 出库确认入参（签收信息）。
 */
@Data
public class DeliveryConfirmDTO {

    private String signUser;
    /** 签收时间，留空默认当前时间。 */
    private LocalDateTime signTime;
    private String remark;
}

package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 待出库期间允许补录或修正的单头信息。 */
@Data
public class DeliveryPendingUpdateDTO {

    @Size(max = 100, message = "收货客户名称不能超过100个字符")
    private String receiverCustomerName;

    @NotNull(message = "出库日期不能为空")
    private LocalDate deliveryDate;

    @Size(max = 50, message = "提货人不能超过50个字符")
    private String pickerName;

    @Size(max = 50, message = "车牌号不能超过50个字符")
    private String carNo;

    @Size(max = 50, message = "柜号不能超过50个字符")
    private String containerNo;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DeliveryBatchConfirmDTO {

    @NotEmpty(message = "出库单不能为空")
    @Size(max = 100, message = "单次批量签收不能超过100张")
    private List<@NotBlank(message = "出库单uuid不能为空") String> deliveryUuids;

    @Size(max = 50, message = "签收人不能超过50个字符")
    private String signUser;

    @PastOrPresent(message = "签收时间不能晚于当前时间")
    private LocalDateTime signTime;

    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;

    public DeliveryConfirmDTO confirmData() {
        DeliveryConfirmDTO data = new DeliveryConfirmDTO();
        data.setSignUser(signUser);
        data.setSignTime(signTime);
        data.setRemark(remark);
        return data;
    }
}

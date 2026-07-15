package com.paper.mes.delivery.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 待出库改单时追加成品明细。只允许追加同客户、已入库、未被其他待出库单锁定的成品卷。
 */
@Data
public class DeliveryAppendItemsDTO {

    /** 现结客户存在未结清款项时，前端确认后传 true；后端默认按配置警告放行或强制拦截。 */
    private boolean forceRelease;

    @NotEmpty(message = "追加成品不能为空")
    @Size(max = 500, message = "单次追加成品不能超过500条")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        @NotBlank(message = "成品uuid不能为空")
        private String finishUuid;
        /** 本件出库重量，留空则取成品实际重量。 */
        @Positive(message = "出库重量必须大于0")
        private BigDecimal outWeight;
        private String remark;
    }
}

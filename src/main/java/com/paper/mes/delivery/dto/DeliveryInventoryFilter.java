package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryInventoryFilter {

    @Size(max = 64, message = "客户标识不能超过64个字符")
    private String customerUuid;

    @Size(max = 64, message = "仓库标识不能超过64个字符")
    private String warehouseUuid;

    @Size(max = 64, message = "加工单标识不能超过64个字符")
    private String orderUuid;

    @Size(max = 100, message = "搜索关键字不能超过100个字符")
    private String keyword;

    /** 1 可出库，2 待出库占用。 */
    @Min(value = 1, message = "库存状态无效")
    @Max(value = 2, message = "库存状态无效")
    private Integer stockState;

    /** 1 普通成品，2 余料，3 原纸直发。 */
    @Min(value = 1, message = "库存类型无效")
    @Max(value = 3, message = "库存类型无效")
    private Integer inventoryType;

    @Min(value = 0, message = "最小库龄不能小于0天")
    @Max(value = 36500, message = "最小库龄不能超过36500天")
    private Integer stockAgeMinDays;
}

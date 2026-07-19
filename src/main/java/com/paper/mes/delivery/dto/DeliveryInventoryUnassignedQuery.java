package com.paper.mes.delivery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DeliveryInventoryUnassignedQuery {

    @Size(max = 100, message = "搜索关键词不能超过100个字符")
    private String keyword;

    @Min(value = 1, message = "页码不能小于1")
    private long current = 1;

    @Min(value = 1, message = "每页数量不能小于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private long size = 20;
}

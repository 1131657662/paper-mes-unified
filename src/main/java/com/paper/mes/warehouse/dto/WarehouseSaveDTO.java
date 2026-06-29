package com.paper.mes.warehouse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 仓库新增/修改入参。
 */
@Data
public class WarehouseSaveDTO {

    @Size(max = 50, message = "仓库编码长度不能超过50")
    private String warehouseCode;

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 100, message = "仓库名称长度不能超过100")
    private String warehouseName;

    private String location;
    /** 1启用 2停用，不传默认启用 */
    private Integer status;
    private String remark;
}

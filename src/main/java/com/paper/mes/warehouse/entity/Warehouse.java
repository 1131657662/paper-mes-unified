package com.paper.mes.warehouse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.paper.mes.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 仓库档案 sys_warehouse。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_warehouse")
public class Warehouse extends BaseEntity {

    @TableId(type = IdType.ASSIGN_UUID)
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;

    private String warehouseCode;
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private String warehouseName;
    /** 仓库地址或识别说明，不表示库位层级。 */
    private String location;
    /** 1启用 2停用 */
    private Integer status;
    private Integer isDefault;
    private String remark;
}

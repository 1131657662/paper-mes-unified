package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 工序新增/修改 DTO
 */
@Data
public class ProcessStepDTO {
    /**
     * 工序UUID（修改时必填）
     */
    private String uuid;

    /**
     * 原纸卷UUID
     */
    @NotBlank(message = "原纸卷UUID不能为空")
    private String originalUuid;

    /**
     * 工序类型：1锯纸 2复卷
     */
    @NotNull(message = "工序类型不能为空")
    private Integer stepType;

    /**
     * 工序名称
     */
    private String stepName;

    /**
     * 是否主工艺：1主 0追加
     */
    private Integer isMain;

    /**
     * 工序排序号（自动分配，可选）
     */
    private Integer stepSort;

    /**
     * 锯纸刀数
     */
    private Integer knifeCount;

    /**
     * 复卷吨位
     */
    private BigDecimal processWeight;

    /**
     * 单价（元/刀 或 元/吨）
     */
    private BigDecimal unitPrice;

    /**
     * 备注
     */
    private String remark;
}

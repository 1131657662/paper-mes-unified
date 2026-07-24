package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
     * 工序类型：1锯纸 2复卷 3剥损整理 4重新包装
     */
    @NotNull(message = "工序类型不能为空")
    private Integer stepType;

    /**
     * 工序名称
     */
    @Size(max = 50, message = "工序名称不能超过50个字符")
    private String stepName;
    private String machineUuid;

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

    @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{0,15}", message = "计费单位代码格式不正确")
    private String billingBasis;

    @DecimalMin(value = "0.001", message = "服务数量必须大于0")
    private BigDecimal serviceQuantity;

    @Min(value = 1, message = "计费模式无效")
    @Max(value = 4, message = "计费模式无效")
    private Integer billingMode;

    @DecimalMin(value = "0.00", message = "固定金额不能为负数")
    private BigDecimal billingAmount;

    /**
     * 单价（元/刀 或 元/吨）
     */
    private BigDecimal unitPrice;

    /**
     * 备注
     */
    @Size(max = 255, message = "备注不能超过255个字符")
    private String remark;
}

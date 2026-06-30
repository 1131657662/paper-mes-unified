package com.paper.mes.system.config.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoRuleSaveDTO {

    @NotBlank(message = "业务类型不能为空")
    @Size(max = 50, message = "业务类型长度不能超过50")
    private String bizType;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 100, message = "规则名称长度不能超过100")
    private String ruleName;

    @NotBlank(message = "前缀不能为空")
    @Size(max = 20, message = "前缀长度不能超过20")
    private String prefix;

    @NotNull(message = "格式类型不能为空")
    @Min(value = 1, message = "格式类型不正确")
    @Max(value = 2, message = "格式类型不正确")
    private Integer patternType;

    @Size(max = 20, message = "日期格式长度不能超过20")
    private String datePattern;

    @NotNull(message = "流水位数不能为空")
    @Min(value = 3, message = "流水位数不能小于3")
    @Max(value = 10, message = "流水位数不能超过10")
    private Integer serialLength;

    @NotNull(message = "重置周期不能为空")
    @Min(value = 0, message = "重置周期不正确")
    @Max(value = 3, message = "重置周期不正确")
    private Integer resetCycle;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态不正确")
    @Max(value = 1, message = "状态不正确")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

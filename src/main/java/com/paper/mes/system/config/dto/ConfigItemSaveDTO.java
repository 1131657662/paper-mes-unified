package com.paper.mes.system.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigItemSaveDTO {

    @NotBlank(message = "参数分组不能为空")
    @Size(max = 50, message = "参数分组长度不能超过50")
    private String configGroup;

    @NotBlank(message = "参数键不能为空")
    @Size(max = 80, message = "参数键长度不能超过80")
    private String configKey;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 80, message = "参数名称长度不能超过80")
    private String configName;

    @NotBlank(message = "参数值不能为空")
    @Size(max = 255, message = "参数值长度不能超过255")
    private String configValue;

    @NotBlank(message = "值类型不能为空")
    @Size(max = 20, message = "值类型长度不能超过20")
    private String valueType;

    @Size(max = 20, message = "单位长度不能超过20")
    private String unit;

    private Integer sortNo;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

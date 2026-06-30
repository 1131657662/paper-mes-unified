package com.paper.mes.system.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DictItemSaveDTO {

    @NotBlank(message = "字典分类不能为空")
    @Size(max = 50, message = "字典分类长度不能超过50")
    private String dictType;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 80, message = "分类名称长度不能超过80")
    private String dictName;

    @NotBlank(message = "字典编码不能为空")
    @Size(max = 50, message = "字典编码长度不能超过50")
    private String itemCode;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 80, message = "字典名称长度不能超过80")
    private String itemName;

    private Integer itemValue;
    private Integer sortNo;

    @NotNull(message = "状态不能为空")
    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

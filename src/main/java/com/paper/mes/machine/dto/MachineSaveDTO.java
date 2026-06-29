package com.paper.mes.machine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机台新增/修改入参。
 */
@Data
public class MachineSaveDTO {

    @Size(max = 50, message = "机台编码长度不能超过50")
    private String machineCode;

    @NotBlank(message = "机台名称不能为空")
    @Size(max = 100, message = "机台名称长度不能超过100")
    private String machineName;

    /** 机台类型 1锯纸 2复卷 3通用 */
    private Integer machineType;
    /** 1启用 2停用，不传默认启用 */
    private Integer status;
    private String remark;
}

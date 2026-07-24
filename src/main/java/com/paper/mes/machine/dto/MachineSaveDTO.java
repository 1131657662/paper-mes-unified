package com.paper.mes.machine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

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
    @Min(value = 1, message = "历史机台类型无效")
    @Max(value = 3, message = "历史机台类型无效")
    private Integer machineType;
    @Pattern(regexp = "MACHINE|WORKSTATION", message = "生产资源类型无效")
    private String resourceKind;
    /** 1启用 2停用，不传默认启用 */
    @Min(value = 1, message = "机台状态无效")
    @Max(value = 2, message = "机台状态无效")
    private Integer status;
    @Valid
    @Size(max = 50, message = "工艺能力不能超过50项")
    private List<MachineCapabilitySaveDTO> capabilities;
    private String remark;
}

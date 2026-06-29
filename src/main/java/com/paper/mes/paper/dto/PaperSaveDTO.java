package com.paper.mes.paper.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 纸张新增/修改入参。
 */
@Data
public class PaperSaveDTO {

    @Size(max = 50, message = "纸张编码长度不能超过50")
    private String paperCode;

    @NotBlank(message = "纸张品名不能为空")
    @Size(max = 100, message = "纸张品名长度不能超过100")
    private String paperName;

    private Integer gramWeight;
    private String paperType;
    private String remark;
}

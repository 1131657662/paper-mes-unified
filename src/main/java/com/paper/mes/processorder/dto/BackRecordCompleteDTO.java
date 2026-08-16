package com.paper.mes.processorder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

/** Closes a back-record order without rewriting any historical roll data. */
@Data
public class BackRecordCompleteDTO implements BackRecordClosureApproval {

    @NotNull(message = "单据版本不能为空")
    @Min(value = 0, message = "单据版本不能小于0")
    private Integer expectedVersion;

    @Size(max = 50, message = "管理员账号长度不能超过50")
    private String releaseAdminUsername;

    @ToString.Exclude
    @Size(max = 128, message = "管理员密码长度不能超过128")
    private String releaseAdminPassword;

    @Size(max = 500, message = "放行原因长度不能超过500")
    private String releaseReason;

    @Size(max = 500, message = "偏差原因长度不能超过500")
    private String varianceReason;
}

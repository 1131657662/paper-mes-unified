package com.paper.mes.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.paper.mes.auth.service.PasswordPolicy;

@Data
public class UserSaveDTO {

    @NotBlank(message = "登录账号不能为空")
    @Size(max = 50, message = "登录账号长度不能超过50")
    private String username;

    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50")
    private String realName;

    @NotBlank(message = "角色不能为空")
    private String roleCode;

    private Integer status;

    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

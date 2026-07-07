package com.paper.mes.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import com.paper.mes.auth.service.PasswordPolicy;

@Data
public class ChangePasswordDTO {

    @NotBlank(message = "原密码不能为空")
    @Size(max = 100, message = "原密码过长")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    private String newPassword;
}

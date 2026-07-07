package com.paper.mes.system.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import com.paper.mes.auth.service.PasswordPolicy;

@Data
public class UserPasswordDTO {

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
    private String password;
}

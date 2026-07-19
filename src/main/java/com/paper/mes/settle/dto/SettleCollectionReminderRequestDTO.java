package com.paper.mes.settle.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SettleCollectionReminderRequestDTO {
    @NotBlank(message = "请求号不能为空")
    @Size(max = 64, message = "请求号不能超过64个字符")
    private String requestId;
    @NotNull(message = "提醒渠道不能为空")
    @Min(value = 1, message = "提醒渠道不正确")
    @Max(value = 5, message = "提醒渠道不正确")
    private Integer reminderChannel;
    @NotNull(message = "提醒结果不能为空")
    @Min(value = 1, message = "提醒结果不正确")
    @Max(value = 5, message = "提醒结果不正确")
    private Integer reminderResult;
    @Size(max = 100, message = "联系人不能超过100个字符")
    private String contactName;
    @PastOrPresent(message = "提醒时间不能晚于当前时间")
    private LocalDateTime reminderTime;
    private LocalDate nextFollowUpDate;
    @NotBlank(message = "提醒记录不能为空")
    @Size(max = 500, message = "提醒记录不能超过500个字符")
    private String remark;
}

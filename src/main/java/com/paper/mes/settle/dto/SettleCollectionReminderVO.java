package com.paper.mes.settle.dto;

import com.paper.mes.settle.entity.SettleCollectionReminder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettleCollectionReminderVO(
        String uuid,
        Integer reminderChannel,
        Integer reminderResult,
        String contactName,
        LocalDateTime reminderTime,
        LocalDate nextFollowUpDate,
        String operatorName,
        String remark) {

    public static SettleCollectionReminderVO from(SettleCollectionReminder item) {
        return new SettleCollectionReminderVO(item.getUuid(), item.getReminderChannel(), item.getReminderResult(),
                item.getContactName(), item.getReminderTime(), item.getNextFollowUpDate(),
                item.getOperatorName(), item.getRemark());
    }
}

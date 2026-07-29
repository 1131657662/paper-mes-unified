package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;

import java.time.LocalDateTime;

final class BackRecordVoidedFinishWriter {

    private BackRecordVoidedFinishWriter() {
    }

    static void write(FinishRollMapper mapper, FinishRoll finish) {
        LocalDateTime now = LocalDateTime.now();
        UpdateWrapper<FinishRoll> update = new UpdateWrapper<FinishRoll>()
                .eq("uuid", finish.getUuid())
                .set("roll_no_status", finish.getRollNoStatus())
                .set("finish_status", finish.getFinishStatus())
                .set("production_result", finish.getProductionResult())
                .set("production_adjustment_reason", finish.getProductionAdjustmentReason())
                .set("actual_weight", null)
                .set("remaining_weight", null)
                .set("scrap_weight", null)
                .set("is_abnormal", 0)
                .set("abnormal_type", null)
                .set("actual_remark", finish.getActualRemark())
                .set("stock_in_time", null)
                .set("update_time", now);
        applyVersion(update, finish);
        ConcurrencyGuard.requireRowUpdated(mapper.update(null, update));
        finish.setUpdateTime(now);
    }

    private static void applyVersion(UpdateWrapper<FinishRoll> update, FinishRoll finish) {
        if (finish.getVersion() == null) {
            update.setSql("version = COALESCE(version, 0) + 1");
            return;
        }
        int currentVersion = finish.getVersion();
        update.eq("version", currentVersion).set("version", currentVersion + 1);
        finish.setVersion(currentVersion + 1);
    }
}

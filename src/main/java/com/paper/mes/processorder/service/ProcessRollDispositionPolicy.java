package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRollDispositionDTO;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.model.ProcessRollDispositionAction;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/** Pure guards for post-issue source-roll disposition. */
public final class ProcessRollDispositionPolicy {
    private static final int PROCESSING = 2;
    private static final int TO_RECORD = 3;
    private static final int CHECKED = 1;
    private static final int ROLL_DONE = 3;
    private static final int DIRECT_SHIP = 4;
    private static final int SCRAPPED = 5;

    private ProcessRollDispositionPolicy() {
    }

    public static void requireOrderEditable(ProcessOrder order) {
        if (order == null || (order.getOrderStatus() != PROCESSING && order.getOrderStatus() != TO_RECORD)) {
            throw new BusinessException("仅加工中或待回录单据允许处置未加工母卷");
        }
    }

    public static void requireRollEditable(OriginalRoll roll) {
        if (roll == null || roll.getDispositionAction() != null
                || Integer.valueOf(CHECKED).equals(roll.getIsChecked())
                || Integer.valueOf(ROLL_DONE).equals(roll.getRollStatus())
                || Integer.valueOf(DIRECT_SHIP).equals(roll.getRollStatus())
                || Integer.valueOf(SCRAPPED).equals(roll.getRollStatus())) {
            throw new BusinessException("母卷已回录或已处置，不能重复处置");
        }
    }

    public static void requireCommand(ProcessRollDispositionDTO dto) {
        if (dto == null || dto.getAction() == null) {
            throw new BusinessException("处置动作不能为空");
        }
        if (dto.getExpectedOrderVersion() == null || dto.getExpectedOrderVersion() < 0) {
            throw new BusinessException("单据版本无效，请刷新后重试");
        }
        if (!StringUtils.hasText(dto.getRequestId())) {
            throw new BusinessException("幂等请求号不能为空");
        }
        if (dto.getRequestId().trim().length() > 64) {
            throw new BusinessException("幂等请求号不能超过64个字符");
        }
        if (!StringUtils.hasText(dto.getReason())) {
            throw new BusinessException("处置原因不能为空");
        }
        if (dto.getReason().trim().length() > 500) {
            throw new BusinessException("处置原因不能超过500个字符");
        }
        if (dto.getWarehouseUuid() != null && dto.getWarehouseUuid().trim().length() > 64) {
            throw new BusinessException("入库仓库标识不能超过64个字符");
        }
        if (dto.getAction() == ProcessRollDispositionAction.DIRECT_SHIP
                && !positive(dto.getActualWeight())) {
            throw new BusinessException("转直发必须填写现场称重总重量");
        }
    }

    public static boolean positive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}

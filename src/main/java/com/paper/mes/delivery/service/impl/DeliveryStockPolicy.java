package com.paper.mes.delivery.service.impl;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.entity.FinishRoll;

import java.math.BigDecimal;

/**
 * 出库库存重量规则：只做纯计算和校验，不直接写库。
 */
final class DeliveryStockPolicy {

    private static final int FINISH_STATUS_OUT = 3;
    private static final int SOURCE_TYPE_DIRECT = 2;

    private DeliveryStockPolicy() {
    }

    static BigDecimal availableWeight(FinishRoll finish) {
        if (finish == null) {
            return BigDecimal.ZERO;
        }
        if (finish.getFinishStatus() != null && finish.getFinishStatus() == FINISH_STATUS_OUT) {
            return BigDecimal.ZERO;
        }
        if (finish.getRemainingWeight() != null) {
            return finish.getRemainingWeight();
        }
        return nz(finish.getActualWeight());
    }

    static void validateOutWeight(FinishRoll finish, BigDecimal outWeight) {
        if (outWeight == null || outWeight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("出库重量必须大于 0：" + finish.getFinishRollNo());
        }
        BigDecimal available = availableWeight(finish);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("成品无可出库重量：" + finish.getFinishRollNo());
        }
        if (outWeight.compareTo(available) > 0) {
            throw new BusinessException("出库重量不能大于剩余可出库重量：" + finish.getFinishRollNo());
        }
        if (isDirectFinish(finish) && outWeight.compareTo(available) != 0) {
            throw new BusinessException("直发原纸必须整卷出库：" + finish.getFinishRollNo());
        }
    }

    static BigDecimal remainingAfterConfirm(FinishRoll finish, BigDecimal outWeight) {
        validateOutWeight(finish, outWeight);
        return availableWeight(finish).subtract(outWeight);
    }

    static BigDecimal remainingAfterRollback(FinishRoll finish, BigDecimal outWeight) {
        BigDecimal restored = availableWeight(finish).add(nz(outWeight));
        BigDecimal actual = nz(finish.getActualWeight());
        if (actual.compareTo(BigDecimal.ZERO) > 0 && restored.compareTo(actual) > 0) {
            throw new BusinessException("回退出库后剩余重量不能大于件重：" + finish.getFinishRollNo());
        }
        return restored;
    }

    private static boolean isDirectFinish(FinishRoll finish) {
        return finish.getSourceType() != null && finish.getSourceType() == SOURCE_TYPE_DIRECT;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

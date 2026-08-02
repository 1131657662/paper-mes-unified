package com.paper.mes.inventory.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.mapper.DeliveryDetailMapper;
import com.paper.mes.inventory.dto.InventoryScrapDTO;
import com.paper.mes.oplog.service.OperationLogService;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinishedGoodsScrapService {

    private static final int IN_STOCK = 2;
    private static final int SCRAPPED = 4;

    private final FinishRollMapper finishRollMapper;
    private final DeliveryDetailMapper deliveryDetailMapper;
    private final BusinessLockService businessLockService;
    private final InventoryLedgerBusinessRecorder ledgerRecorder;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class)
    public void scrap(String finishUuid, InventoryScrapDTO dto) {
        requireText(finishUuid, "finishUuid");
        if (dto == null) {
            throw new BusinessException("scrap command is required");
        }
        requireText(dto.getReason(), "reason");
        requireText(dto.getRequestUuid(), "requestUuid");
        businessLockService.lockFinishRolls(List.of(finishUuid));
        FinishRoll finish = finishRollMapper.selectById(finishUuid);
        if (finish == null) {
            throw new BusinessException(ErrorCode.E002, "成品卷不存在");
        }
        if (!Integer.valueOf(IN_STOCK).equals(finish.getFinishStatus())) {
            throw new BusinessException(ErrorCode.E004, "仅已入库且仍有库存的成品卷允许报废");
        }
        long blocking = deliveryDetailMapper.countBlockingDeliveryActivity(List.of(finishUuid));
        if (blocking > 0) {
            throw new BusinessException(ErrorCode.E004, "成品卷存在出库占用或出库记录，不可报废");
        }
        BigDecimal remaining = finish.getRemainingWeight();
        if (remaining == null || remaining.signum() <= 0) {
            throw new BusinessException(ErrorCode.E004, "成品卷当前无可报废库存");
        }
        String reason = dto.getReason().trim();
        String requestUuid = dto.getRequestUuid().trim();
        ledgerRecorder.scrap(finish, requestUuid, reason, remaining, LocalDateTime.now());
        ConcurrencyGuard.requireRowUpdated(finishRollMapper.update(null,
                new LambdaUpdateWrapper<FinishRoll>()
                        .eq(FinishRoll::getUuid, finishUuid)
                        .eq(FinishRoll::getFinishStatus, IN_STOCK)
                        .set(FinishRoll::getFinishStatus, SCRAPPED)
                        .set(FinishRoll::getRemainingWeight, BigDecimal.ZERO)
                        .set(FinishRoll::getActualRemark, reason)
                        .set(FinishRoll::getUpdateTime, LocalDateTime.now())
                        .setSql("version = version + 1")));
        operationLogService.record(OperationLogService.BIZ_TYPE_ORDER, finishUuid,
                finish.getFinishRollNo(), OperationLogService.ACTION_INVENTORY_SCRAP,
                null, reason + "（请求号：" + requestUuid + "）");
    }

    private void requireText(String value, String name) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(name + "不能为空");
        }
    }
}

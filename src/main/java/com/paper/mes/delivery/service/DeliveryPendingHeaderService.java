package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ConcurrencyGuard;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.common.db.BusinessLockService;
import com.paper.mes.delivery.dto.DeliveryPendingUpdateDTO;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryOrderMapper;
import com.paper.mes.oplog.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeliveryPendingHeaderService {

    private static final int DELIVERY_STATUS_PENDING = 1;

    private final DeliveryOrderMapper deliveryOrderMapper;
    private final BusinessLockService businessLockService;
    private final OperationLogService operationLogService;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void update(String uuid, DeliveryPendingUpdateDTO dto) {
        businessLockService.lockDeliveryOrder(uuid);
        DeliveryOrder order = requirePendingOrder(uuid);
        normalize(dto);
        persist(uuid, dto);
        recordChanges(order, dto);
    }

    private DeliveryOrder requirePendingOrder(String uuid) {
        DeliveryOrder order = deliveryOrderMapper.selectById(uuid);
        if (order == null) {
            throw new BusinessException(ErrorCode.E002, "出库单不存在");
        }
        if (!Objects.equals(order.getDeliveryStatus(), DELIVERY_STATUS_PENDING)) {
            throw new BusinessException("仅待出库单允许编辑出库信息");
        }
        return order;
    }

    private void normalize(DeliveryPendingUpdateDTO dto) {
        dto.setReceiverCustomerName(trimToNull(dto.getReceiverCustomerName()));
        dto.setPickerName(trimToNull(dto.getPickerName()));
        dto.setCarNo(trimToNull(dto.getCarNo()));
        dto.setContainerNo(trimToNull(dto.getContainerNo()));
        dto.setRemark(trimToNull(dto.getRemark()));
    }

    private void persist(String uuid, DeliveryPendingUpdateDTO dto) {
        LambdaUpdateWrapper<DeliveryOrder> update = new LambdaUpdateWrapper<DeliveryOrder>()
                .eq(DeliveryOrder::getUuid, uuid)
                .eq(DeliveryOrder::getDeliveryStatus, DELIVERY_STATUS_PENDING)
                .set(DeliveryOrder::getReceiverCustomerName, dto.getReceiverCustomerName())
                .set(DeliveryOrder::getDeliveryDate, dto.getDeliveryDate())
                .set(DeliveryOrder::getPickerName, dto.getPickerName())
                .set(DeliveryOrder::getCarNo, dto.getCarNo())
                .set(DeliveryOrder::getContainerNo, dto.getContainerNo())
                .set(DeliveryOrder::getRemark, dto.getRemark())
                .set(DeliveryOrder::getUpdateTime, LocalDateTime.now())
                .set(DeliveryOrder::getUpdateBy, AuthContextHolder.currentDisplayName())
                .setSql("version = version + 1");
        ConcurrencyGuard.requireRowUpdated(deliveryOrderMapper.update(null, update));
    }

    private void recordChanges(DeliveryOrder order, DeliveryPendingUpdateDTO dto) {
        for (HeaderChange change : changes(order, dto)) {
            if (change.changed()) {
                operationLogService.recordField(OperationLogService.BIZ_TYPE_DELIVERY,
                        order.getUuid(), order.getDeliveryNo(), change.label(),
                        change.oldValue(), change.newValue(), null);
            }
        }
    }

    private List<HeaderChange> changes(DeliveryOrder order, DeliveryPendingUpdateDTO dto) {
        return List.of(
                change("收货客户", order.getReceiverCustomerName(), dto.getReceiverCustomerName()),
                change("出库日期", order.getDeliveryDate(), dto.getDeliveryDate()),
                change("提货人", order.getPickerName(), dto.getPickerName()),
                change("车牌号", order.getCarNo(), dto.getCarNo()),
                change("柜号", order.getContainerNo(), dto.getContainerNo()),
                change("备注", order.getRemark(), dto.getRemark()));
    }

    private HeaderChange change(String label, Object oldValue, Object newValue) {
        return new HeaderChange(label, text(oldValue), text(newValue));
    }

    private String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record HeaderChange(String label, String oldValue, String newValue) {
        private boolean changed() {
            return !Objects.equals(oldValue, newValue);
        }
    }
}

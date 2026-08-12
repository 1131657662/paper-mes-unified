package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.FinishCustomerSpecVO;
import com.paper.mes.processorder.entity.ProcessOrder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/** Keeps commercial customer-display revisions separate from frozen production instructions. */
@Component
public class FinishCustomerRevisionPolicy {

    private static final int STATUS_PROCESSING = 2;
    private static final int STATUS_TO_RECORD = 3;
    private static final int STATUS_SETTLED = 5;
    private static final int STATUS_VOIDED = 6;

    public void requireReadable(ProcessOrder order) {
        if (Integer.valueOf(STATUS_VOIDED).equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.E001, "已作废加工单不能维护客户规格");
        }
    }

    public boolean requiresReissue(ProcessOrder order, List<FinishCustomerSpecVO> rows) {
        return Integer.valueOf(STATUS_PROCESSING).equals(order.getOrderStatus())
                && changesPrintedSpecification(rows);
    }

    public void requirePublishAllowed(ProcessOrder order, List<FinishCustomerSpecVO> rows) {
        requireReadable(order);
        if (!changesPrintedSpecification(rows)) return;
        Integer status = order.getOrderStatus();
        if (status != null && status >= STATUS_TO_RECORD && status <= STATUS_SETTLED) {
            throw new BusinessException(ErrorCode.E003,
                    "待回录、已完成或已结算加工单不能修改已下发的客户品名、克重或门幅");
        }
    }

    private boolean changesPrintedSpecification(List<FinishCustomerSpecVO> rows) {
        return rows.stream().anyMatch(this::changesPrintedSpecification);
    }

    private boolean changesPrintedSpecification(FinishCustomerSpecVO row) {
        return !Objects.equals(row.getPreviousCustomerPaperName(), row.getCustomerPaperName())
                || !Objects.equals(row.getPreviousCustomerGramWeight(), row.getCustomerGramWeight())
                || !Objects.equals(row.getPreviousCustomerFinishWidth(), row.getCustomerFinishWidth());
    }
}

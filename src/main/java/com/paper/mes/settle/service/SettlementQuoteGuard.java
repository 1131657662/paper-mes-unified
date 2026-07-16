package com.paper.mes.settle.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.settle.dto.SettleQuoteVO;
import com.paper.mes.settle.entity.SettleOrder;
import org.springframework.stereotype.Component;

@Component
public class SettlementQuoteGuard {
    public void verify(String version, String hash, SettleQuoteVO current) {
        if (!current.getQuoteVersion().equals(version) || !current.getQuoteHash().equalsIgnoreCase(hash)) {
            throw new BusinessException("报价已变化，请刷新试算后重新提交");
        }
    }

    public void verifyIdempotentReplay(SettleOrder existing, String version, String hash) {
        if (!same(existing.getQuoteVersion(), version) || !same(existing.getQuoteHash(), hash)) {
            throw new BusinessException("请求号已用于其他结算，请刷新后重试");
        }
    }

    private boolean same(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}

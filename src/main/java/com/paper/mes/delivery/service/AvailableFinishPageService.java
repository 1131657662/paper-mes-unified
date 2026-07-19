package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.paper.mes.common.PageRequestBounds;
import com.paper.mes.common.PageResult;
import com.paper.mes.delivery.dto.AvailableFinishQuery;
import com.paper.mes.delivery.dto.AvailableFinishPageVO;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.delivery.mapper.AvailableFinishMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvailableFinishPageService {

    private final AvailableFinishMapper mapper;
    private final AvailableFinishSourceLoader sourceLoader;
    private final DeliveryCashSettlementGuard cashSettlementGuard;

    public AvailableFinishPageVO page(AvailableFinishQuery query) {
        Page<AvailableFinishVO> page = PageRequestBounds.of(query.getCurrent(), query.getSize());
        page.setTotal(mapper.count(query));
        List<AvailableFinishVO> rows = mapper.rows(query, page.offset(), page.getSize());
        enrich(rows);
        page.setRecords(rows);
        return AvailableFinishPageVO.of(PageResult.of(page), mapper.stats(query), LocalDateTime.now());
    }

    private void enrich(List<AvailableFinishVO> rows) {
        if (rows.isEmpty()) return;
        List<FinishRoll> finishes = rows.stream().map(this::finishIdentity).toList();
        var sources = sourceLoader.load(finishes);
        Set<String> risks = cashSettlementGuard.unsettledCashOrderUuids(rows.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getSettleType()))
                .map(AvailableFinishVO::getOrderUuid)
                .collect(Collectors.toSet()));
        rows.forEach(item -> {
            item.setSourceMotherRolls(sources.getOrDefault(item.getFinishUuid(), List.of()));
            item.setSettlementRisk(risks.contains(item.getOrderUuid()));
        });
    }

    private FinishRoll finishIdentity(AvailableFinishVO item) {
        FinishRoll finish = new FinishRoll();
        finish.setUuid(item.getFinishUuid());
        return finish;
    }
}

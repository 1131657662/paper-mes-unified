package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainApplicationVO;
import com.paper.mes.remain.entity.RemainApplication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RemainApplicationQueryService {

    public RemainApplicationVO toView(RemainApplication source) {
        RemainApplicationVO result = new RemainApplicationVO();
        result.setUuid(source.getUuid());
        result.setRegistrationUuid(source.getRegistrationUuid());
        result.setSettleUuid(source.getSettleUuid());
        result.setAdjustmentUuid(source.getAdjustmentUuid());
        result.setReceiveUuid(source.getReceiveUuid());
        result.setStatus(source.getStatus());
        result.setAmount(source.getAmount());
        result.setWeight(source.getWeight());
        return result;
    }
}

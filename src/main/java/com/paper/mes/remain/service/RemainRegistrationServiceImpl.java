package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainRegistrationCreateDTO;
import com.paper.mes.remain.dto.RemainRegistrationQuery;
import com.paper.mes.remain.dto.RemainRegistrationVO;
import com.paper.mes.remain.dto.RemainRollbackDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RemainRegistrationServiceImpl implements RemainRegistrationService {

    private final RemainRegistrationCommandService commandService;
    private final RemainRollbackCommandService rollbackService;
    private final RemainRegistrationQueryService queryService;

    @Override
    public RemainRegistrationVO create(RemainRegistrationCreateDTO request) {
        return queryService.detail(commandService.create(request).getUuid());
    }

    @Override
    public RemainRegistrationVO detail(String uuid) {
        return queryService.detail(uuid);
    }

    @Override
    public List<RemainRegistrationVO> list(RemainRegistrationQuery query) {
        return queryService.list(query);
    }

    @Override
    public RemainRegistrationVO rollback(String uuid, RemainRollbackDTO request) {
        return queryService.detail(rollbackService.rollback(uuid, request).getUuid());
    }
}

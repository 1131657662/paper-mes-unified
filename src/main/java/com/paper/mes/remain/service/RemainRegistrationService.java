package com.paper.mes.remain.service;

import com.paper.mes.remain.dto.RemainRegistrationCreateDTO;
import com.paper.mes.remain.dto.RemainRegistrationQuery;
import com.paper.mes.remain.dto.RemainRegistrationVO;
import com.paper.mes.remain.dto.RemainRollbackDTO;

import java.util.List;

public interface RemainRegistrationService {

    RemainRegistrationVO create(RemainRegistrationCreateDTO request);

    RemainRegistrationVO detail(String uuid);

    List<RemainRegistrationVO> list(RemainRegistrationQuery query);

    RemainRegistrationVO rollback(String uuid, RemainRollbackDTO request);
}

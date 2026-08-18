package com.paper.mes.ai.process.status;

import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessAiAvailabilityGuard {

    private final ProcessAiStatusService statusService;

    public void requireReady() {
        ProcessAiStatusResponse status = statusService.status();
        if (!status.ready()) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                    status.unavailableReason(), "AI工艺助手尚未就绪");
        }
    }
}

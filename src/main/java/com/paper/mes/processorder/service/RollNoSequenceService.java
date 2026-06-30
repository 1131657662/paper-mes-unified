package com.paper.mes.processorder.service;

import com.paper.mes.system.config.constant.NoRuleBizType;
import com.paper.mes.system.config.service.DocumentNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RollNoSequenceService {

    private final DocumentNoService documentNoService;

    @Transactional(rollbackFor = Exception.class)
    public String nextFinishRollNo() {
        return documentNoService.next(NoRuleBizType.FINISH_ROLL, LocalDate.now());
    }
}

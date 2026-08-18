package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiCompilationResult;
import com.paper.mes.ai.process.compile.ProcessAiPlanCompilationService;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmRequest;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProcessAiParseConfirmationService {

    private final ProcessAiConfirmationPreparationService preparationService;
    private final ProcessAiPlanCompilationService compilationService;
    private final ProcessAiConfirmationCommitter committer;

    @Transactional
    public ProcessAiConfirmResponse confirm(String orderUuid, ProcessAiConfirmRequest request) {
        ProcessAiConfirmationPreparation preparation = preparationService.prepare(
                orderUuid, request);
        if (preparation.isReplay()) return preparation.load().replay();
        ProcessAiCompilationResult compilation = compilationService.compile(
                preparation.load().extraction(), preparation.context(),
                preparation.redaction().charges());
        requireEligible(compilation);
        return committer.commit(preparation, compilation);
    }

    private void requireEligible(ProcessAiCompilationResult compilation) {
        if (!compilation.eligible()) {
            throw new BusinessException(ResultCode.CONFLICT,
                    "AI_PARSE_NOT_APPLICABLE", "AI candidate no longer passes process preview");
        }
    }
}

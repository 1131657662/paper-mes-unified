package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.model.ProcessAiProviderException;
import com.paper.mes.common.BusinessException;
import org.springframework.dao.DataAccessException;

record ProcessAiFailure(String code, String message, boolean retryable) {

    static ProcessAiFailure classify(RuntimeException exception) {
        if (exception instanceof ProcessAiProviderException provider) {
            return new ProcessAiFailure(
                    provider.failureCode(), provider.getMessage(), provider.retryable());
        }
        if (exception instanceof BusinessException business) {
            String code = business.getErrorCode() == null
                    ? "AI_PROCESS_REJECTED" : business.getErrorCode();
            return new ProcessAiFailure(code, business.getMessage(), false);
        }
        if (exception instanceof DataAccessException) {
            return new ProcessAiFailure("AI_PARSE_PERSIST_FAILED",
                    "AI解析结果保存失败，请稍后重试", true);
        }
        return new ProcessAiFailure("AI_PROCESS_INTERNAL_ERROR", "AI工艺解析失败", true);
    }
}

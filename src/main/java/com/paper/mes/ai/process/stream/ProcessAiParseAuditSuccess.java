package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.parse.ProcessAiParseRecord;

record ProcessAiParseAuditSuccess(
        ProcessAiPreparedParse prepared,
        ProcessAiModelExecution execution,
        ProcessAiParseRecord parseRecord,
        String outcome) {
}

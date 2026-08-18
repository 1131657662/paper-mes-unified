package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.process.intent.ProcessAiAssignment;

record ProjectMemoryCandidateProposal(
        String memoryId,
        String candidateType,
        String scope,
        String intent,
        String phrase,
        ObjectNode document,
        ProcessAiAssignment assignment) {
}

package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.process.session.ProcessAiConversationService;
import com.paper.mes.ai.process.session.dto.ReserveProcessAiParseCommand;
import com.paper.mes.ai.process.status.ProcessAiAvailabilityGuard;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseResultResponse;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
class ProcessAiParseStreamGate {

    private final ProcessAiConversationService conversationService;
    private final ProcessAiParseReplayService replayService;
    private final ProcessAiAvailabilityGuard availabilityGuard;

    Optional<ProcessAiParseResultResponse> replay(String orderUuid,
                                                  ProcessAiParseStreamRequest request) {
        availabilityGuard.requireReady();
        conversationService.requireAccess(new ReserveProcessAiParseCommand(
                orderUuid, request.conversationId(), request.expectedVersion()));
        return replayService.replay(request.conversationId(), request.idempotencyKey());
    }
}

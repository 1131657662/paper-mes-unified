package com.paper.mes.ai.process.stream;

import com.paper.mes.ai.config.AiProperties;
import com.paper.mes.ai.process.status.ProcessAiDialogueV2Feature;
import com.paper.mes.ai.process.stream.dto.ProcessAiParseStreamRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class ProcessAiParseStreamService {

    private final ProcessAiParsePreparationService preparationService;
    private final ProcessAiParseStreamGate streamGate;
    private final ProcessAiParseAsyncRunner asyncRunner;
    private final ProcessAiSingleFlightRegistry singleFlightRegistry;
    private final ProcessAiHeartbeatScheduler heartbeatScheduler;
    private final AiProperties properties;
    private final ProcessAiDialogueV2Feature dialogueV2Feature;

    public SseEmitter start(String orderUuid, ProcessAiParseStreamRequest request) {
        var replay = streamGate.replay(orderUuid, request);
        if (replay.isPresent()) {
            ProcessAiSseSink sink = new ProcessAiSseSink(properties.processStreamTimeoutMs());
            heartbeatScheduler.register(sink);
            sink.conversation(request.conversationId());
            sink.result(replay.get());
            sink.done();
            return sink.emitter();
        }
        dialogueV2Feature.requireEnabled(orderUuid);
        ProcessAiSseSink sink = new ProcessAiSseSink(properties.processStreamTimeoutMs());
        heartbeatScheduler.register(sink);
        sink.conversation(request.conversationId());
        ProcessAiSingleFlightRegistry.Registration registration =
                singleFlightRegistry.register(request, sink);
        if (!registration.owner()) return sink.emitter();
        try {
            ProcessAiPreparedParse prepared = preparationService.prepare(orderUuid, request);
            asyncRunner.submit(prepared, registration.sink(), registration.cancellation());
        } catch (RuntimeException ex) {
            ProcessAiFailure failure = ProcessAiFailure.classify(ex);
            registration.sink().error(failure.code(), failure.message(), failure.retryable());
            registration.sink().done();
        }
        return sink.emitter();
    }
}

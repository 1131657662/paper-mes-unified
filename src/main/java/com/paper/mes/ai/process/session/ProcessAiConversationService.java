package com.paper.mes.ai.process.session;

import com.paper.mes.ai.memory.ProjectMemoryDocumentProvider;
import com.paper.mes.ai.memory.ProjectMemorySnapshot;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.session.dto.ProcessAiSessionRequest;
import com.paper.mes.ai.process.session.dto.ProcessAiSessionResponse;
import com.paper.mes.ai.process.stream.ProcessAiSingleFlightRegistry;
import com.paper.mes.ai.process.session.dto.ProcessAiParseReservation;
import com.paper.mes.ai.process.session.dto.ReserveProcessAiParseCommand;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessAiConversationService {

    private final CloudDbContextReader contextReader;
    private final ProjectMemoryDocumentProvider memoryProvider;
    private final ProcessAiConversationRepository repository;
    private final ProcessAiSingleFlightRegistry singleFlightRegistry;

    @Transactional
    public ProcessAiSessionResponse open(String orderUuid, ProcessAiSessionRequest request) {
        contextReader.read(orderUuid, request.expectedVersion());
        CurrentUser user = requireCurrentUser();
        ProjectMemorySnapshot currentMemory = memoryProvider.current().orElse(null);
        return repository.findByOrderForUpdate(orderUuid)
                .map(row -> resume(row, user, request, currentMemory))
                .orElseGet(() -> create(orderUuid, user, requireMemory(currentMemory), request));
    }

    @Transactional
    public ProcessAiParseReservation reserveParse(ReserveProcessAiParseCommand command) {
        contextReader.read(command.orderUuid(), command.expectedVersion());
        CurrentUser user = requireCurrentUser();
        ProcessAiConversationRow row = requireConversation(command);
        requireOwner(row, user);
        requireOpen(row);
        synchronizeIfBehind(row, command.expectedVersion());
        int revision = repository.reserveNextRevision(command.conversationId());
        if (revision < 1) {
            throw conflict("AI_CONVERSATION_REVISION_CONFLICT",
                    "AI conversation revision could not be reserved");
        }
        return new ProcessAiParseReservation(
                command.conversationId(), revision, row.projectMemoryVersion(),
                row.memoryGeneration());
    }

    @Transactional
    public ProcessAiSessionResponse refreshMemory(String orderUuid, String conversationId,
                                                   int expectedVersion) {
        contextReader.read(orderUuid, expectedVersion);
        ProcessAiConversationRow row = requireByOrder(orderUuid, conversationId, true);
        requireOwner(row, requireCurrentUser());
        requireOpen(row);
        ProcessAiConversationRow current = synchronizeIfBehind(row, expectedVersion);
        if (singleFlightRegistry.isConversationInFlight(conversationId)) {
            throw conflict("AI_PARSE_IN_PROGRESS", "AI解析仍在进行，暂时不能刷新项目记忆");
        }
        ProjectMemorySnapshot memory = requireMemory(memoryProvider.current().orElse(null));
        if (memory.docVersion().equals(current.projectMemoryVersion())) {
            return response(current, true, memory);
        }
        if (repository.refreshMemory(conversationId, current.memoryGeneration(),
                memory.docVersion()) != 1) {
            throw conflict("AI_MEMORY_REFRESH_CONFLICT", "项目记忆刷新发生并发冲突");
        }
        ProcessAiConversationRow refreshed = new ProcessAiConversationRow(
                current.uuid(), current.conversationId(), current.orderUuid(), current.userUuid(),
                current.currentStep(), current.draftVersion(), memory.docVersion(),
                current.memoryGeneration() + 1, "OPEN");
        return response(refreshed, true, memory);
    }

    @Transactional(readOnly = true)
    public void requireAccess(ReserveProcessAiParseCommand command) {
        contextReader.read(command.orderUuid(), command.expectedVersion());
        CurrentUser user = requireCurrentUser();
        ProcessAiConversationRow row = repository.findByOrder(command.orderUuid())
                .filter(value -> value.conversationId().equals(command.conversationId()))
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND,
                        "AI_CONVERSATION_NOT_FOUND", "AI会话不存在"));
        requireOwner(row, user);
        requireOpen(row);
        requireNotAhead(row, command.expectedVersion());
    }

    @Transactional
    public void markInterrupted(String orderUuid, String conversationId) {
        ProcessAiConversationRow row = repository.findByOrderForUpdate(orderUuid)
                .filter(value -> value.conversationId().equals(conversationId))
                .orElse(null);
        if (row == null) return;
        requireOwner(row, requireCurrentUser());
        repository.markInterrupted(conversationId);
    }

    @Transactional(readOnly = true)
    public String requireConfirmationOwner(String orderUuid, String conversationId) {
        CurrentUser user = requireCurrentUser();
        ProcessAiConversationRow row = requireByOrder(orderUuid, conversationId, false);
        requireOwner(row, user);
        return user.getUuid();
    }

    @Transactional
    public void lockForConfirmation(String orderUuid, String conversationId,
                                    int expectedVersion, int memoryGeneration) {
        ProcessAiConversationRow row = requireByOrder(orderUuid, conversationId, true);
        requireOwner(row, requireCurrentUser());
        requireOpen(row);
        requireVersion(row, expectedVersion);
        if (row.memoryGeneration() != memoryGeneration) {
            throw conflict("AI_MEMORY_GENERATION_CONFLICT",
                    "该解析结果使用的是旧项目记忆，请重新解析后再应用");
        }
    }

    @Transactional
    public void advanceDraftVersion(String conversationId, int expectedVersion, int nextVersion) {
        if (repository.advanceDraftVersion(conversationId, expectedVersion, nextVersion) != 1) {
            throw conflict("AI_CONVERSATION_VERSION_CONFLICT",
                    "AI conversation draft version could not be advanced");
        }
    }

    private ProcessAiConversationRow requireConversation(ReserveProcessAiParseCommand command) {
        return repository.findByOrderForUpdate(command.orderUuid())
                .filter(value -> value.conversationId().equals(command.conversationId()))
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND,
                        "AI_CONVERSATION_NOT_FOUND", "AI会话不存在"));
    }

    private ProcessAiConversationRow requireByOrder(String orderUuid, String conversationId,
                                                     boolean forUpdate) {
        return (forUpdate ? repository.findByOrderForUpdate(orderUuid)
                : repository.findByOrder(orderUuid))
                .filter(value -> value.conversationId().equals(conversationId))
                .orElseThrow(() -> new BusinessException(ResultCode.NOT_FOUND,
                        "AI_CONVERSATION_NOT_FOUND", "AI conversation not found"));
    }

    private ProcessAiSessionResponse create(String orderUuid, CurrentUser user,
                                             ProjectMemorySnapshot memory, ProcessAiSessionRequest request) {
        ProcessAiConversationRow row = new ProcessAiConversationRow(
                UUID.randomUUID().toString(), UUID.randomUUID().toString(), orderUuid,
                user.getUuid(), request.currentStep(), request.expectedVersion(),
                memory.docVersion(), 1, "OPEN");
        repository.insert(row);
        return response(row, false, memory);
    }

    private ProcessAiSessionResponse resume(ProcessAiConversationRow row, CurrentUser user,
                                             ProcessAiSessionRequest request,
                                             ProjectMemorySnapshot currentMemory) {
        requireOwner(row, user);
        requireOpen(row);
        ProcessAiConversationRow current = synchronizeIfBehind(row, request.expectedVersion());
        repository.reopen(row.conversationId(), request.currentStep());
        ProcessAiConversationRow resumed = new ProcessAiConversationRow(
                row.uuid(), row.conversationId(), row.orderUuid(), row.userUuid(),
                request.currentStep(), current.draftVersion(), row.projectMemoryVersion(),
                row.memoryGeneration(), "OPEN");
        return response(resumed, true, currentMemory);
    }

    private CurrentUser requireCurrentUser() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null || user.getUuid().isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return user;
    }

    private ProjectMemorySnapshot requireMemory(ProjectMemorySnapshot memory) {
        if (memory != null) return memory;
        throw new BusinessException(
                ResultCode.SERVICE_UNAVAILABLE, "AI_MEMORY_UNAVAILABLE", "项目记忆暂不可用");
    }

    private void requireOwner(ProcessAiConversationRow row, CurrentUser user) {
        if (!row.userUuid().equals(user.getUuid())) {
            throw new BusinessException(ResultCode.FORBIDDEN,
                    "AI_CONVERSATION_FORBIDDEN", "该AI会话属于其他用户");
        }
    }

    private void requireOpen(ProcessAiConversationRow row) {
        if (!"OPEN".equals(row.status()) && !"INTERRUPTED".equals(row.status())) {
            throw conflict("AI_CONVERSATION_CLOSED", "该加工单AI会话已关闭");
        }
    }

    private void requireVersion(ProcessAiConversationRow row, int expectedVersion) {
        if (row.draftVersion() != expectedVersion) {
            throw conflict("AI_CONVERSATION_VERSION_CONFLICT", "AI会话绑定的草稿版本已过期");
        }
    }

    private ProcessAiConversationRow synchronizeIfBehind(ProcessAiConversationRow row,
                                                          int expectedVersion) {
        requireNotAhead(row, expectedVersion);
        if (row.draftVersion() == expectedVersion) return row;
        if (repository.advanceDraftVersion(
                row.conversationId(), row.draftVersion(), expectedVersion) != 1) {
            throw conflict("AI_CONVERSATION_VERSION_CONFLICT",
                    "AI conversation draft version could not be synchronized");
        }
        return new ProcessAiConversationRow(
                row.uuid(), row.conversationId(), row.orderUuid(), row.userUuid(),
                row.currentStep(), expectedVersion, row.projectMemoryVersion(),
                row.memoryGeneration(), "OPEN");
    }

    private void requireNotAhead(ProcessAiConversationRow row, int expectedVersion) {
        if (row.draftVersion() > expectedVersion) {
            throw conflict("AI_CONVERSATION_VERSION_CONFLICT", "AI会话绑定的草稿版本已过期");
        }
    }

    private ProcessAiSessionResponse response(ProcessAiConversationRow row, boolean resumed,
                                               ProjectMemorySnapshot currentMemory) {
        String latestVersion = currentMemory == null ? null : currentMemory.docVersion();
        return new ProcessAiSessionResponse(row.conversationId(), row.status(), row.currentStep(),
                row.draftVersion(), row.projectMemoryVersion(), row.memoryGeneration(),
                latestVersion, latestVersion != null
                        && !latestVersion.equals(row.projectMemoryVersion()), resumed);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}

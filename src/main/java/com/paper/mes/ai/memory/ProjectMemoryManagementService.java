package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryResponse;
import com.paper.mes.ai.memory.dto.ProjectMemoryRollbackRequest;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Orchestrates versioned project-memory changes; persistence stays in the repository. */
@Service
@RequiredArgsConstructor
public class ProjectMemoryManagementService {

    private final ProjectMemoryDocumentProvider provider;
    private final ProjectMemoryDocumentRepository repository;
    private final ProjectMemoryDocumentValidator validator;
    private final ProjectMemoryPatchApplier patchApplier = new ProjectMemoryPatchApplier();
    private final ProjectMemoryDocumentInvariantValidator invariantValidator =
            new ProjectMemoryDocumentInvariantValidator();
    private final ObjectMapper objectMapper;

    public ProjectMemoryResponse current() {
        return provider.current().map(this::response)
                .orElseThrow(() -> new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                        "MEMORY_UNAVAILABLE", "项目记忆当前不可用"));
    }

    public ProjectMemoryResponse reload() {
        return provider.reload().map(this::response)
                .orElseThrow(() -> new BusinessException(ResultCode.SERVICE_UNAVAILABLE,
                        "MEMORY_UNAVAILABLE", "项目记忆当前不可用"));
    }

    @Transactional
    public ProjectMemoryResponse patch(ProjectMemoryPatchRequest request) {
        requirePatchRequest(request);
        ProjectMemoryDocumentRow active = lockedActive();
        Optional<ProjectMemoryResponse> repeated = repeated(request.idempotencyKey());
        if (repeated.isPresent()) return repeated.get();
        requireVersion(request.expectedMemoryVersion(), active.docVersion());
        ProjectMemorySnapshot current = validator.validateDatabaseRow(active);
        ProjectMemorySnapshot next;
        try {
            ObjectNode changed = patchApplier.apply((ObjectNode) current.document(), request.operations());
            String nextVersion = nextAvailableVersion(active.docVersion());
            changed.put("memoryVersion", nextVersion);
            changed.put("checksum", new ProjectMemoryChecksum(objectMapper).calculate(changed));
            invariantValidator.validate(changed);
            next = validator.validateNode(changed, nextVersion, current.schemaVersion(),
                    changed.path("checksum").asText());
        } catch (IllegalArgumentException ex) {
            throw patchError(ex);
        }
        persist(active, next, request.reason(), request.idempotencyKey(), "PATCH",
                request.expectedMemoryVersion(), objectMapper.valueToTree(request.operations()));
        return response(next);
    }

    @Transactional
    public ProjectMemoryResponse rollback(ProjectMemoryRollbackRequest request) {
        requireRollbackRequest(request);
        ProjectMemoryDocumentRow active = lockedActive();
        Optional<ProjectMemoryResponse> repeated = repeated(request.idempotencyKey());
        if (repeated.isPresent()) return repeated.get();
        requireVersion(request.expectedMemoryVersion(), active.docVersion());
        ProjectMemoryDocumentRow target = repository.findVersionForUpdate(request.targetMemoryVersion())
                .orElseThrow(() -> new BusinessException(ResultCode.BAD_REQUEST,
                        "MEMORY_VERSION_NOT_FOUND", "目标记忆版本不存在"));
        if ("DRAFT".equals(target.status())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "MEMORY_ROLLBACK_INVALID", "不能回滚到草稿版本");
        }
        ProjectMemorySnapshot targetSnapshot = validator.validateDatabaseRow(
                new ProjectMemoryDocumentRow(target.uuid(), target.docVersion(), target.schemaVersion(),
                        target.checksum(), target.docJson(), "ACTIVE", target.patchNotes(), target.createdBy(), target.approvedBy()));
        ObjectNode rollback = ((ObjectNode) targetSnapshot.document()).deepCopy();
        String nextVersion = nextAvailableVersion(active.docVersion());
        rollback.put("memoryVersion", nextVersion);
        rollback.put("checksum", new ProjectMemoryChecksum(objectMapper).calculate(rollback));
        ProjectMemorySnapshot snapshot = validator.validateNode(
                rollback, nextVersion, targetSnapshot.schemaVersion(), rollback.path("checksum").asText());
        ObjectNode operation = objectMapper.createObjectNode();
        operation.put("targetMemoryVersion", request.targetMemoryVersion());
        persist(active, snapshot, request.reason(), request.idempotencyKey(), "ROLLBACK",
                request.expectedMemoryVersion(), operation);
        return response(snapshot);
    }

    private void persist(ProjectMemoryDocumentRow active, ProjectMemorySnapshot next, String reason,
                         String idempotencyKey, String operation, String expected, Object operations) {
        requireUpdated(repository.markSuperseded(active.docVersion()), "active memory snapshot");
        repository.insert(new ProjectMemoryDocumentRow(UUID.randomUUID().toString(), next.docVersion(),
                next.schemaVersion(), next.checksum(), next.document().toString(), "ACTIVE", reason,
                operator(), operator()));
        repository.insertAudit(new ProjectMemoryPatchAuditRow(UUID.randomUUID().toString(), idempotencyKey,
                operation, expected, active.docVersion(), next.docVersion(), active.checksum(), next.checksum(),
                operations.toString(), reason, operator()));
        reloadAfterCommit();
    }

    private Optional<ProjectMemoryResponse> repeated(String key) {
        return repository.findAuditByIdempotencyKey(key).flatMap(audit ->
                repository.findVersionForUpdate(audit.newDocVersion()).map(row -> response(
                        validator.validateDatabaseRow(asActive(row)))));
    }

    private ProjectMemoryDocumentRow asActive(ProjectMemoryDocumentRow row) {
        return new ProjectMemoryDocumentRow(row.uuid(), row.docVersion(), row.schemaVersion(), row.checksum(),
                row.docJson(), "ACTIVE", row.patchNotes(), row.createdBy(), row.approvedBy());
    }

    private ProjectMemoryDocumentRow lockedActive() {
        return repository.findActiveForUpdate().orElseThrow(() -> new BusinessException(
                ResultCode.SERVICE_UNAVAILABLE, "MEMORY_UNAVAILABLE", "项目记忆当前不可用"));
    }

    private String nextAvailableVersion(String current) {
        String candidate = ProjectMemoryVersion.next(current);
        while (repository.existsVersion(candidate)) candidate = ProjectMemoryVersion.next(candidate);
        return candidate;
    }

    private void requireVersion(String expected, String actual) {
        if (!actual.equals(expected)) {
            throw new BusinessException(ResultCode.CONFLICT, "MEMORY_VERSION_CONFLICT",
                    "记忆版本已变化，请刷新后重试：当前版本=" + actual);
        }
    }

    private void requirePatchRequest(ProjectMemoryPatchRequest request) {
        if (request == null || request.operations() == null || request.operations().isEmpty()
                || request.operations().size() > 20) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "MEMORY_PATCH_INVALID", "项目记忆补丁操作数无效");
        }
        if (blank(request.expectedMemoryVersion()) || blank(request.idempotencyKey()) || blank(request.reason())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "MEMORY_PATCH_INVALID", "项目记忆补丁参数无效");
        }
    }

    private void requireRollbackRequest(ProjectMemoryRollbackRequest request) {
        if (request == null || blank(request.expectedMemoryVersion()) || blank(request.targetMemoryVersion())
                || blank(request.idempotencyKey()) || blank(request.reason())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "MEMORY_ROLLBACK_INVALID", "项目记忆回滚参数无效");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ProjectMemoryResponse response(ProjectMemorySnapshot snapshot) {
        return new ProjectMemoryResponse(snapshot.docVersion(), snapshot.schemaVersion(), snapshot.checksum(),
                provider.state(), snapshot.document());
    }

    private String operator() {
        return AuthContextHolder.currentDisplayName();
    }

    private void requireUpdated(int count, String target) {
        if (count != 1) throw new IllegalStateException("project memory update did not affect one " + target);
    }

    private void reloadAfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            provider.reload();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                provider.reload();
            }
        });
    }

    private BusinessException patchError(IllegalArgumentException exception) {
        String raw = exception.getMessage() == null ? "" : exception.getMessage();
        String code = Set.of("MEMORY_CONFLICT_BLOCKED", "MEMORY_PATCH_INVALID_PATH", "MEMORY_PATCH_INVALID_FIELD",
                "MEMORY_PATCH_VALUE_REQUIRED", "MEMORY_PATCH_INVALID_POINTER", "MEMORY_PATCH_PATH_NOT_FOUND",
                "MEMORY_PATCH_PARENT_NOT_CONTAINER", "MEMORY_PATCH_INVALID_ARRAY_PATH").stream()
                .filter(raw::startsWith).findFirst().orElse("MEMORY_PATCH_INVALID");
        return new BusinessException(ResultCode.BAD_REQUEST, code, "项目记忆补丁无效");
    }
}

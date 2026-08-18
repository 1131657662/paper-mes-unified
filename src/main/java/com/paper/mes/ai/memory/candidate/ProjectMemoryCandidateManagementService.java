package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemoryManagementService;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateApproveRequest;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateDetailResponse;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateEvidenceResponse;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateRejectRequest;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateResponse;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchOperation;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryResponse;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectMemoryCandidateManagementService {

    private static final Set<String> STATUSES = Set.of(
            "CANDIDATE", "READY", "ACTIVE", "CONFLICT", "REJECTED", "EXPIRED");

    private final ProjectMemoryCandidateRepository repository;
    private final ProjectMemoryManagementService memoryService;
    private final ObjectMapper objectMapper;

    @Transactional
    public List<ProjectMemoryCandidateResponse> list(String status) {
        String normalized = normalizeStatus(status);
        repository.expireCandidates(LocalDateTime.now());
        return repository.list(normalized).stream().map(this::response).toList();
    }

    @Transactional
    public ProjectMemoryCandidateDetailResponse detail(String uuid) {
        ProjectMemoryCandidateRow row = require(uuid);
        List<ProjectMemoryCandidateEvidenceResponse> evidence = repository.listEvidence(uuid)
                .stream().map(this::evidenceResponse).toList();
        return new ProjectMemoryCandidateDetailResponse(response(row), evidence);
    }

    @Transactional
    public ProjectMemoryResponse approve(
            String uuid, ProjectMemoryCandidateApproveRequest request) {
        ProjectMemoryCandidateRow row = require(uuid);
        if (!"READY".equals(row.status())) {
            throw conflict("MEMORY_CANDIDATE_NOT_READY", "Memory candidate is not ready");
        }
        JsonNode candidate = approvedCandidate(row, request.candidate());
        ProjectMemoryResponse memory = memoryService.patch(new ProjectMemoryPatchRequest(
                request.expectedMemoryVersion(), List.of(new ProjectMemoryPatchOperation(
                "add", "/" + targetRoot(row.candidateType()) + "/" + row.memoryId(), candidate)),
                request.idempotencyKey(), request.reason()));
        if (!"ACTIVE".equals(row.status())) {
            requireUpdated(repository.review(row.uuid(), "ACTIVE", operator(),
                    request.reason(), candidate.toString(), LocalDateTime.now()));
        }
        return memory;
    }

    @Transactional
    public ProjectMemoryCandidateResponse reject(
            String uuid, ProjectMemoryCandidateRejectRequest request) {
        ProjectMemoryCandidateRow row = require(uuid);
        if (Set.of("ACTIVE", "REJECTED", "EXPIRED").contains(row.status())) {
            throw conflict("MEMORY_CANDIDATE_REVIEW_CONFLICT", "Memory candidate cannot be rejected");
        }
        requireUpdated(repository.review(row.uuid(), "REJECTED", operator(),
                request.reason(), row.candidateJson(), LocalDateTime.now()));
        return response(repository.findByUuidForUpdate(uuid).orElseThrow());
    }

    private ProjectMemoryCandidateRow require(String uuid) {
        return repository.findByUuidForUpdate(uuid).orElseThrow(() ->
                new BusinessException(ResultCode.NOT_FOUND,
                        "MEMORY_CANDIDATE_NOT_FOUND", "Memory candidate not found"));
    }

    private ProjectMemoryCandidateResponse response(ProjectMemoryCandidateRow row) {
        return new ProjectMemoryCandidateResponse(
                row.uuid(), row.memoryId(), row.candidateType(), parse(row.candidateJson()),
                row.status(), row.distinctOrderCount(), row.firstSeenAt(), row.lastSeenAt(),
                row.expiresAt(), row.reviewedBy(), row.reviewNotes(), row.reviewedAt());
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("memory candidate JSON is invalid", exception);
        }
    }

    private ProjectMemoryCandidateEvidenceResponse evidenceResponse(
            ProjectMemoryCandidateEvidenceRow row) {
        return new ProjectMemoryCandidateEvidenceResponse(
                row.uuid(), row.orderUuid(), row.orderNo(), row.parseId(), row.sourceType(),
                row.phrase(), parseNullable(row.contextJson()),
                parseNullable(row.proposedValueJson()), parseNullable(row.finalValueJson()),
                parseNullable(row.differenceJson()), row.previewReady(), row.createdBy(),
                row.createdAt());
    }

    private JsonNode parseNullable(String json) {
        return json == null ? null : parse(json);
    }

    private JsonNode approvedCandidate(ProjectMemoryCandidateRow row, JsonNode edited) {
        JsonNode candidate = edited == null ? parse(row.candidateJson()) : edited;
        if (!candidate.isObject()
                || !row.candidateType().equals(candidate.path("type").asText())
                || !"ACTIVE".equals(candidate.path("status").asText())) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "MEMORY_CANDIDATE_EDIT_INVALID", "候选知识的类型或状态无效");
        }
        return candidate;
    }

    private String targetRoot(String candidateType) {
        return switch (candidateType) {
            case "TERM" -> "terms";
            case "EXAMPLE" -> "examples";
            case "RULE" -> "rules";
            default -> throw new BusinessException(ResultCode.BAD_REQUEST,
                    "MEMORY_CANDIDATE_TYPE_NOT_APPROVABLE", "该类型不能进入正式项目记忆");
        };
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) return null;
        String normalized = status.trim().toUpperCase();
        if (!STATUSES.contains(normalized)) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "MEMORY_CANDIDATE_STATUS_INVALID", "Memory candidate status is invalid");
        }
        return normalized;
    }

    private String operator() {
        return AuthContextHolder.currentDisplayName();
    }

    private void requireUpdated(int count) {
        if (count != 1) throw new IllegalStateException("memory candidate review failed");
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}

package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ProjectMemoryCandidateManagementService {

    private static final Set<String> STATUSES = Set.of(
            "CANDIDATE", "READY", "ACTIVE", "CONFLICT", "REJECTED", "EXPIRED");

    private final ProjectMemoryCandidateRepository repository;
    private final ProjectMemoryManagementService memoryService;
    private final ObjectMapper objectMapper;
    private final ProjectMemoryCandidateDocumentValidator candidateValidator;

    @Autowired
    public ProjectMemoryCandidateManagementService(ProjectMemoryCandidateRepository repository,
                                                   ProjectMemoryManagementService memoryService,
                                                   ObjectMapper objectMapper,
                                                   ProjectMemoryCandidateDocumentValidator candidateValidator) {
        this.repository = repository;
        this.memoryService = memoryService;
        this.objectMapper = objectMapper;
        this.candidateValidator = candidateValidator;
    }

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
                .stream().map(item -> evidenceResponse(row, item)).toList();
        return new ProjectMemoryCandidateDetailResponse(response(row), evidence);
    }

    @Transactional
    public ProjectMemoryResponse approve(
            String uuid, ProjectMemoryCandidateApproveRequest request) {
        ProjectMemoryCandidateRow row = require(uuid);
        if (!"READY".equals(row.status())) {
            throw conflict("MEMORY_CANDIDATE_NOT_READY", "Memory candidate is not ready");
        }
        String root = targetRoot(row.candidateType());
        JsonNode candidate = approvedCandidate(row, request.candidate());
        ProjectMemoryResponse memory = memoryService.patch(new ProjectMemoryPatchRequest(
                request.expectedMemoryVersion(), List.of(new ProjectMemoryPatchOperation(
                "add", "/" + root + "/" + row.memoryId(), candidate)),
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
        JsonNode candidate = parse(row.candidateJson());
        candidate = responseCandidate(row.candidateType(), candidate);
        return new ProjectMemoryCandidateResponse(
                row.uuid(), row.memoryId(), row.candidateType(), candidate,
                row.status(), row.distinctOrderCount(), row.firstSeenAt(), row.lastSeenAt(),
                row.expiresAt(), row.reviewedBy(), row.reviewNotes(), row.reviewedAt());
    }

    private JsonNode responseCandidate(String candidateType, JsonNode candidate) {
        if (Set.of("TERM", "EXAMPLE").contains(candidateType)) {
            candidateValidator.validateSharedText(candidate);
            return candidate;
        }
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("type", candidateType);
        summary.put("status", candidate.path("status").asText("UNKNOWN"));
        summary.put("legacy", true);
        return summary;
    }

    private JsonNode parse(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            throw new IllegalStateException("memory candidate JSON is invalid", exception);
        }
    }

    private ProjectMemoryCandidateEvidenceResponse evidenceResponse(
            ProjectMemoryCandidateRow candidate, ProjectMemoryCandidateEvidenceRow row) {
        return new ProjectMemoryCandidateEvidenceResponse(
                row.uuid(), sharedPhrase(candidate), row.sourceType(),
                parseNullable(row.proposedValueJson()), parseNullable(row.finalValueJson()),
                parseNullable(row.differenceJson()), row.previewReady(),
                row.createdAt());
    }

    private String sharedPhrase(ProjectMemoryCandidateRow row) {
        JsonNode candidate = parse(row.candidateJson());
        return switch (row.candidateType()) {
            case "TERM" -> candidate.path("phrase").asText(null);
            case "EXAMPLE" -> candidate.path("input").asText(null);
            default -> null;
        };
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
        candidateValidator.validate(row.candidateType(), candidate);
        return candidate;
    }

    private String targetRoot(String candidateType) {
        return switch (candidateType) {
            case "TERM" -> "terms";
            case "EXAMPLE" -> "examples";
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

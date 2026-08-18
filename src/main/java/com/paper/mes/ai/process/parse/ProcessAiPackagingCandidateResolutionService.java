package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.dto.ProcessStepDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProcessAiPackagingCandidateResolutionService {

    private final CloudDbContextReader contextReader;
    private final PermissionChecker permissionChecker;
    private final ProcessAiPackagingCandidateRepository repository;

    public void dismiss(String orderUuid, String parseId, String ownerRollRef,
                        int expectedVersion) {
        contextReader.read(orderUuid, expectedVersion);
        resolve(orderUuid, new CandidateKey(parseId, ownerRollRef), "DISMISSED");
    }

    public void markSaved(String orderUuid, List<ProcessStepDTO> steps) {
        Set<CandidateKey> keys = sourceKeys(steps);
        if (keys.isEmpty()) return;
        for (CandidateKey key : keys) resolve(orderUuid, key, "SAVED");
    }

    private Set<CandidateKey> sourceKeys(List<ProcessStepDTO> steps) {
        Set<CandidateKey> keys = new LinkedHashSet<>();
        for (ProcessStepDTO step : steps) {
            boolean hasParse = StringUtils.hasText(step.getAiParseId());
            boolean hasOwner = StringUtils.hasText(step.getAiOwnerRollRef());
            if (hasParse != hasOwner) throw invalidSource();
            if (hasParse) keys.add(new CandidateKey(
                    step.getAiParseId(), step.getAiOwnerRollRef()));
        }
        return keys;
    }

    private void resolve(String orderUuid, CandidateKey key, String targetStatus) {
        permissionChecker.require(Permissions.AI_ASSIST);
        String userUuid = currentUserUuid();
        int updated = repository.resolve(orderUuid, key.parseId(), key.ownerRollRef(),
                userUuid, targetStatus, userUuid);
        if (updated == 1) return;
        String status = repository.findStatus(orderUuid, key.parseId(),
                        key.ownerRollRef(), userUuid)
                .orElseThrow(this::notFound);
        if (targetStatus.equals(status)) return;
        throw conflict(status);
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null || user.getUuid().isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return user.getUuid();
    }

    private BusinessException invalidSource() {
        return new BusinessException(ResultCode.BAD_REQUEST,
                "AI_PACKAGING_SOURCE_INVALID", "AI包装候选来源不完整");
    }

    private BusinessException notFound() {
        return new BusinessException(ResultCode.NOT_FOUND,
                "AI_PACKAGING_CANDIDATE_NOT_FOUND", "AI包装候选不存在");
    }

    private BusinessException conflict(String status) {
        return new BusinessException(ResultCode.CONFLICT,
                "AI_PACKAGING_CANDIDATE_RESOLVED", "AI包装候选已结案：" + status);
    }

    private record CandidateKey(String parseId, String ownerRollRef) {
    }
}

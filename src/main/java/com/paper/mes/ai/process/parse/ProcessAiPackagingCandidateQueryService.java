package com.paper.mes.ai.process.parse;

import com.paper.mes.ai.process.compile.ProcessAiPackagingCandidate;
import com.paper.mes.ai.process.context.CloudDbContextReader;
import com.paper.mes.ai.process.parse.dto.ProcessAiConfirmResponse;
import com.paper.mes.ai.process.parse.dto.ProcessAiPendingPackagingCandidate;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.auth.dto.CurrentUser;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProcessAiPackagingCandidateQueryService {

    private final CloudDbContextReader contextReader;
    private final PermissionChecker permissionChecker;
    private final ProcessAiPackagingCandidateRepository repository;
    private final ProcessAiConfirmationCodec codec;

    public List<ProcessAiPendingPackagingCandidate> pending(String orderUuid,
                                                             int expectedVersion) {
        contextReader.read(orderUuid, expectedVersion);
        permissionChecker.require(Permissions.AI_ASSIST);
        String userUuid = currentUserUuid();
        return repository.findPending(orderUuid, userUuid).stream()
                .map(this::toResponse)
                .toList();
    }

    private ProcessAiPendingPackagingCandidate toResponse(ProcessAiPackagingCandidateRow row) {
        ProcessAiConfirmResponse response = codec.readResponse(
                row.confirmedResultJson(), row.conversationId(), row.parseRevision());
        ProcessAiPackagingCandidate candidate = response.packagingCandidates().stream()
                .filter(value -> value.ownerRollRef().equals(row.ownerRollRef()))
                .filter(value -> value.originalUuid().equals(row.originalUuid()))
                .findFirst()
                .orElseThrow(this::corrupted);
        return new ProcessAiPendingPackagingCandidate(row.parseId(), candidate);
    }

    private String currentUserUuid() {
        CurrentUser user = AuthContextHolder.getCurrentUser();
        if (user == null || user.getUuid() == null || user.getUuid().isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "请先登录");
        }
        return user.getUuid();
    }

    private BusinessException corrupted() {
        return new BusinessException(ResultCode.ERROR,
                "AI_PACKAGING_CANDIDATE_INVALID", "AI包装候选数据不完整");
    }
}

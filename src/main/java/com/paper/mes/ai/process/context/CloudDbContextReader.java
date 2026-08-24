package com.paper.mes.ai.process.context;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.auth.permission.PermissionChecker;
import com.paper.mes.auth.permission.Permissions;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessOrder;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import com.paper.mes.processorder.mapper.ProcessOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reads the current process draft through the existing application datasource. */
@Service
@RequiredArgsConstructor
public class CloudDbContextReader {

    private static final int DRAFT_STATUS = 0;
    private static final int MAX_SOURCE_PIECES = 100;

    private final ProcessOrderMapper orderMapper;
    private final OriginalRollMapper rollMapper;
    private final PermissionChecker permissionChecker;
    private final ProcessAiDraftBaselineReader baselineReader;

    @Transactional(readOnly = true)
    public ProcessAiOrderContext read(String orderUuid, int expectedVersion) {
        requirePermissions();
        ProcessOrder order = requireCurrentDraft(orderUuid, expectedVersion);
        List<OriginalRoll> rolls = loadRolls(orderUuid);
        requireSupportedPieceCount(rolls);
        List<ProcessAiRollContext> contexts = mapRolls(rolls);
        ProcessAiReviewBaseline baseline = new ProcessAiReviewBaseline(
                order.getRemarkLong(), baselineReader.read(orderUuid, contexts));
        return new ProcessAiOrderContext(
                orderUuid, expectedVersion, order.getRemarkLong(), contexts, baseline);
    }

    private void requirePermissions() {
        permissionChecker.require(Permissions.ORDER_CREATE);
        permissionChecker.require(Permissions.AI_ASSIST);
    }

    private ProcessOrder requireCurrentDraft(String orderUuid, int expectedVersion) {
        if (orderUuid == null || orderUuid.isBlank() || expectedVersion < 0) {
            throw badRequest("AI_PROCESS_REQUEST_INVALID", "加工单和草稿版本不能为空");
        }
        ProcessOrder order = orderMapper.selectById(orderUuid);
        if (order == null || Integer.valueOf(1).equals(order.getIsDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND,
                    "AI_PROCESS_ORDER_NOT_FOUND", "加工单不存在");
        }
        if (!Integer.valueOf(DRAFT_STATUS).equals(order.getOrderStatus())) {
            throw conflict("AI_PROCESS_NOT_DRAFT", "只有草稿加工单可以使用工艺解析");
        }
        if (!Integer.valueOf(expectedVersion).equals(order.getVersion())) {
            throw conflict("AI_PROCESS_VERSION_CONFLICT", "加工单草稿版本已变化，请重新加载");
        }
        return order;
    }

    private List<OriginalRoll> loadRolls(String orderUuid) {
        return rollMapper.selectList(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .eq(OriginalRoll::getIsDeleted, 0)
                .orderByAsc(OriginalRoll::getRowSort));
    }

    private void requireSupportedPieceCount(List<OriginalRoll> rolls) {
        if (rolls.isEmpty()) {
            throw badRequest("AI_PROCESS_ROLLS_EMPTY", "请先完成第2步母卷录入");
        }
        int pieces = rolls.stream().mapToInt(this::pieceCount).sum();
        if (pieces > MAX_SOURCE_PIECES) {
            throw badRequest("AI_PROCESS_ROLL_LIMIT", "AI工艺解析最多支持100件母卷");
        }
    }

    private List<ProcessAiRollContext> mapRolls(List<OriginalRoll> rolls) {
        return java.util.stream.IntStream.range(0, rolls.size())
                .mapToObj(index -> mapRoll(rolls.get(index), index + 1))
                .toList();
    }

    private ProcessAiRollContext mapRoll(OriginalRoll roll, int index) {
        return new ProcessAiRollContext("R" + index, roll.getUuid(), roll.getRowSort(),
                roll.getPaperName(), roll.getGramWeight(), roll.getOriginalWidth(),
                roll.getOriginalDiameter(), roll.getCoreDiameter(), roll.getRollWeight(),
                roll.getPieceNum(), roll.getProcessMode(), roll.getMainStepType(),
                roll.getActualWeight(), roll.getTotalWeight());
    }

    private int pieceCount(OriginalRoll roll) {
        return roll.getPieceNum() == null ? 1 : Math.max(roll.getPieceNum(), 0);
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}

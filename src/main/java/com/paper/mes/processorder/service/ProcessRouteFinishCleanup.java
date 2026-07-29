package com.paper.mes.processorder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.FinishRollMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 按来源 UUID 清理工艺路线的旧成品，历史文本来源仅做无歧义兼容。 */
@Component
@RequiredArgsConstructor
public class ProcessRouteFinishCleanup {

    private static final int ROLL_NO_VOID = 3;
    private static final int SOURCE_DIRECT_SHIP = 2;

    private final FinishRollMapper finishRollMapper;
    private final FinishOriginalRelMapper relationMapper;
    private final OriginalRollMapper originalRollMapper;

    public void clear(ProcessRouteContext context) {
        OriginalRoll roll = context.roll();
        List<FinishOriginalRel> targetRelations = relationMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .eq(FinishOriginalRel::getOrderUuid, context.order().getUuid())
                        .eq(FinishOriginalRel::getOriginalUuid, roll.getUuid()));
        Set<String> targetIds = targetRelations.stream().map(FinishOriginalRel::getFinishUuid)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<FinishRoll> owned = targetIds.isEmpty()
                ? legacyOwnedFinishes(context) : relatedOwnedFinishes(context, roll, targetIds);
        requireNotRecorded(owned);
        relationMapper.delete(new LambdaQueryWrapper<FinishOriginalRel>()
                .eq(FinishOriginalRel::getOriginalUuid, roll.getUuid()));
        voidFinishes(owned);
    }

    private List<FinishRoll> relatedOwnedFinishes(ProcessRouteContext context, OriginalRoll roll,
                                                   Set<String> targetIds) {
        List<FinishRoll> finishes = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, context.order().getUuid())
                .in(FinishRoll::getUuid, targetIds));
        List<FinishOriginalRel> relations = relationMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>().in(FinishOriginalRel::getFinishUuid, targetIds));
        Map<String, Set<String>> sourcesByFinish = relations.stream().collect(Collectors.groupingBy(
                FinishOriginalRel::getFinishUuid,
                Collectors.mapping(FinishOriginalRel::getOriginalUuid, Collectors.toSet())));
        boolean activeShared = finishes.stream().anyMatch(finish -> isActive(finish)
                && sourcesByFinish.getOrDefault(finish.getUuid(), Set.of()).size() > 1);
        if (activeShared) {
            throw new BusinessException(ErrorCode.E003,
                    "该母卷正被其他母卷的合并复卷配置使用，请从配置拥有母卷重新配置");
        }
        return finishes.stream().filter(this::isActive)
                .filter(finish -> sourcesByFinish.getOrDefault(finish.getUuid(), Set.of())
                        .equals(Set.of(roll.getUuid())))
                .toList();
    }

    private List<FinishRoll> legacyOwnedFinishes(ProcessRouteContext context) {
        OriginalRoll roll = context.roll();
        String sourceKey = sourceKey(roll);
        List<FinishRoll> candidates = finishRollMapper.selectList(new LambdaQueryWrapper<FinishRoll>()
                .eq(FinishRoll::getOrderUuid, context.order().getUuid())
                .eq(FinishRoll::getOriginalRollNos, sourceKey)
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .and(w -> w.isNull(FinishRoll::getSourceType)
                        .or().ne(FinishRoll::getSourceType, SOURCE_DIRECT_SHIP)));
        if (candidates.isEmpty()) {
            return candidates;
        }
        requireUniqueLegacySource(context.order().getUuid(), roll);
        List<String> candidateIds = candidates.stream().map(FinishRoll::getUuid).toList();
        Long relationCount = relationMapper.selectCount(new LambdaQueryWrapper<FinishOriginalRel>()
                .in(FinishOriginalRel::getFinishUuid, candidateIds));
        if (relationCount != null && relationCount > 0) {
            throw new BusinessException(ErrorCode.E003, "历史成品来源关系不一致，请先修复来源数据");
        }
        return candidates;
    }

    private void requireUniqueLegacySource(String orderUuid, OriginalRoll roll) {
        if (!StringUtils.hasText(roll.getRollNo())) {
            return;
        }
        Long count = originalRollMapper.selectCount(new LambdaQueryWrapper<OriginalRoll>()
                .eq(OriginalRoll::getOrderUuid, orderUuid)
                .eq(OriginalRoll::getRollNo, roll.getRollNo()));
        if (count == null || count != 1L) {
            throw new BusinessException(ErrorCode.E003, "历史成品来源卷号重复，无法安全重新配置");
        }
    }

    private void requireNotRecorded(List<FinishRoll> finishes) {
        if (finishes.stream().anyMatch(finish -> finish.getActualWeight() != null)) {
            throw new BusinessException(ErrorCode.E003, "已有回录重量的成品不可重新配置后续工艺");
        }
    }

    private void voidFinishes(List<FinishRoll> finishes) {
        if (finishes.isEmpty()) {
            return;
        }
        List<String> ids = finishes.stream().map(FinishRoll::getUuid).toList();
        int updated = finishRollMapper.update(null, new LambdaUpdateWrapper<FinishRoll>()
                .in(FinishRoll::getUuid, ids)
                .ne(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .isNull(FinishRoll::getActualWeight)
                .set(FinishRoll::getRollNoStatus, ROLL_NO_VOID)
                .set(FinishRoll::getUpdateTime, LocalDateTime.now())
                .set(FinishRoll::getUpdateBy, AuthContextHolder.currentDisplayName())
                .setSql("version = version + 1"));
        if (updated != ids.size()) {
            throw new BusinessException(ErrorCode.E006, "旧成品卷号状态已变化，请刷新后重试");
        }
    }

    private boolean isActive(FinishRoll finish) {
        return !Integer.valueOf(ROLL_NO_VOID).equals(finish.getRollNoStatus());
    }

    private String sourceKey(OriginalRoll roll) {
        return StringUtils.hasText(roll.getRollNo()) ? roll.getRollNo() : roll.getUuid();
    }
}

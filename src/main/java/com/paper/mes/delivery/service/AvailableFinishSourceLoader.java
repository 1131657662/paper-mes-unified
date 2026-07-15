package com.paper.mes.delivery.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.paper.mes.delivery.dto.AvailableFinishVO;
import com.paper.mes.processorder.entity.FinishOriginalRel;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.mapper.FinishOriginalRelMapper;
import com.paper.mes.processorder.mapper.OriginalRollMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AvailableFinishSourceLoader {

    private final FinishOriginalRelMapper finishOriginalRelMapper;
    private final OriginalRollMapper originalRollMapper;

    public Map<String, List<AvailableFinishVO.SourceMotherRollVO>> load(List<FinishRoll> finishes) {
        List<String> finishUuids = finishes.stream()
                .map(FinishRoll::getUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (finishUuids.isEmpty()) {
            return Map.of();
        }
        List<FinishOriginalRel> relations = finishOriginalRelMapper.selectList(
                new LambdaQueryWrapper<FinishOriginalRel>()
                        .in(FinishOriginalRel::getFinishUuid, finishUuids));
        Map<String, OriginalRoll> originals = loadOriginals(relations);
        return groupSources(relations, originals);
    }

    private Map<String, OriginalRoll> loadOriginals(List<FinishOriginalRel> relations) {
        List<String> originalUuids = relations.stream()
                .map(FinishOriginalRel::getOriginalUuid)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        if (originalUuids.isEmpty()) {
            return Map.of();
        }
        return originalRollMapper.selectBatchIds(originalUuids).stream()
                .collect(Collectors.toMap(OriginalRoll::getUuid, Function.identity(), (left, right) -> left));
    }

    private Map<String, List<AvailableFinishVO.SourceMotherRollVO>> groupSources(
            List<FinishOriginalRel> relations, Map<String, OriginalRoll> originals) {
        Map<String, List<AvailableFinishVO.SourceMotherRollVO>> grouped = new LinkedHashMap<>();
        for (FinishOriginalRel relation : relations) {
            OriginalRoll original = originals.get(relation.getOriginalUuid());
            if (original == null || !StringUtils.hasText(relation.getFinishUuid())) {
                continue;
            }
            grouped.computeIfAbsent(relation.getFinishUuid(), key -> new ArrayList<>())
                    .add(toSource(original, relation));
        }
        grouped.values().forEach(items -> items.sort(sourceComparator()));
        return grouped;
    }

    private AvailableFinishVO.SourceMotherRollVO toSource(
            OriginalRoll original, FinishOriginalRel relation) {
        AvailableFinishVO.SourceMotherRollVO source = new AvailableFinishVO.SourceMotherRollVO();
        source.setOriginalUuid(original.getUuid());
        source.setRowSort(original.getRowSort());
        source.setRollNo(original.getRollNo());
        source.setExtraNo(original.getExtraNo());
        source.setPaperName(original.getPaperName());
        source.setGramWeight(original.getActualGramWeight() != null
                ? original.getActualGramWeight() : original.getGramWeight());
        source.setOriginalWidth(original.getActualWidth() != null
                ? original.getActualWidth() : original.getOriginalWidth());
        source.setActualWeight(original.getActualWeight());
        source.setAllocationWeight(relation.getShareWeight());
        return source;
    }

    private Comparator<AvailableFinishVO.SourceMotherRollVO> sourceComparator() {
        return Comparator.comparing(AvailableFinishVO.SourceMotherRollVO::getRowSort,
                        Comparator.nullsLast(Integer::compareTo))
                .thenComparing(AvailableFinishVO.SourceMotherRollVO::getRollNo,
                        Comparator.nullsLast(String::compareTo));
    }
}

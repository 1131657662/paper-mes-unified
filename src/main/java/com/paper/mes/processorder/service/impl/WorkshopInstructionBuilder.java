package com.paper.mes.processorder.service.impl;

import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.WorkshopInstructionVO;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WorkshopInstructionBuilder {

    private WorkshopInstructionBuilder() {
    }

    static List<WorkshopInstructionVO> build(List<ProcessOrderDetailVO.RollProductionVO> productions) {
        Map<GroupKey, InstructionGroup> groups = new LinkedHashMap<>();
        for (ProcessOrderDetailVO.RollProductionVO production : safe(productions)) {
            String instruction = WorkshopProcessInstructionFormatter.format(production);
            if (instruction == null) continue;
            GroupKey key = new GroupKey(production.getOriginalWidth(), instruction);
            groups.computeIfAbsent(key, InstructionGroup::new).add(production);
        }
        return groups.values().stream().map(InstructionGroup::result).toList();
    }

    private static List<ProcessOrderDetailVO.RollProductionVO> safe(
            List<ProcessOrderDetailVO.RollProductionVO> values) {
        return values == null ? List.of() : values;
    }

    private record GroupKey(Integer width, String instruction) {
    }

    private static final class InstructionGroup {
        private final GroupKey key;
        private final List<Integer> rows = new ArrayList<>();
        private int pieces;

        private InstructionGroup(GroupKey key) {
            this.key = key;
        }

        private void add(ProcessOrderDetailVO.RollProductionVO production) {
            if (production.getRowSort() != null) rows.add(production.getRowSort());
            pieces += Math.max(1, production.getPieceNum() == null ? 1 : production.getPieceNum());
        }

        private WorkshopInstructionVO result() {
            rows.sort(Integer::compareTo);
            String text = sourceLabel(key.width(), rows, pieces) + "：" + key.instruction();
            return new WorkshopInstructionVO(rows, key.width(), pieces, key.instruction(), text);
        }
    }

    private static String sourceLabel(Integer width, List<Integer> rows, int pieces) {
        String widthLabel = width == null ? "指定母卷" : width + "mm母卷";
        return widthLabel + rowLabel(rows) + "，共" + pieces + "件";
    }

    private static String rowLabel(List<Integer> rows) {
        if (rows.isEmpty()) return "";
        if (rows.size() == 1) return "（R" + rows.getFirst() + "）";
        if (contiguous(rows)) return "（R" + rows.getFirst() + "-R" + rows.getLast() + "）";
        return "（" + rows.stream().map(row -> "R" + row)
                .collect(java.util.stream.Collectors.joining("、")) + "）";
    }

    private static boolean contiguous(List<Integer> rows) {
        if (rows.size() < 2) return false;
        for (int index = 1; index < rows.size(); index++) {
            if (rows.get(index) != rows.get(index - 1) + 1) return false;
        }
        return true;
    }
}

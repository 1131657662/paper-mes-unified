package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.BackRecordFinishDTO;
import com.paper.mes.processorder.entity.FinishRoll;
import com.paper.mes.processorder.entity.OriginalRoll;

import java.util.List;

final class BackRecordFinishRules {

    private static final int PROCESS_MODE_ON_SITE = 2;
    private static final int SOURCE_DIRECT_SHIP = 2;
    private static final int SPARE = 1;
    private static final int VOID = 3;

    private BackRecordFinishRules() {
    }

    static boolean requiresRecord(FinishRoll finish) {
        return !Integer.valueOf(SOURCE_DIRECT_SHIP).equals(finish.getSourceType())
                && !Integer.valueOf(VOID).equals(finish.getRollNoStatus());
    }

    static boolean unusedSpare(FinishRoll finish, BackRecordFinishDTO dto) {
        return Integer.valueOf(SPARE).equals(finish.getIsSpare()) && dto.getActualWeight() == null;
    }

    static void requireUnusedSpareBlank(FinishRoll finish, BackRecordFinishDTO dto) {
        if (dto.getFinishWidth() != null || dto.getFinishDiameter() != null
                || dto.getFinishCoreDiameter() != null) {
            throw new BusinessException("备用卷填写规格后必须填写实际重量：" + finish.getFinishRollNo());
        }
    }

    static void requireActualWeight(FinishRoll finish, BackRecordFinishDTO dto) {
        if (dto.getActualWeight() == null || dto.getActualWeight().signum() <= 0) {
            throw new BusinessException("成品实际重量必须大于0：" + finish.getFinishRollNo());
        }
    }

    static void requireSources(FinishRoll finish, List<OriginalRoll> sources) {
        if (sources.isEmpty()) {
            throw new BusinessException("成品缺少来源母卷，请先绑定来源：" + finish.getFinishRollNo());
        }
    }

    static boolean onSiteSources(FinishRoll finish, List<OriginalRoll> sources) {
        boolean hasOnSite = sources.stream().anyMatch(BackRecordFinishRules::isOnSite);
        boolean allOnSite = sources.stream().allMatch(BackRecordFinishRules::isOnSite);
        if (hasOnSite != allOnSite) {
            throw new BusinessException("成品来源母卷加工方式不一致：" + finish.getFinishRollNo());
        }
        return allOnSite;
    }

    static void validateWidth(FinishRoll finish, BackRecordFinishDTO dto,
                              List<OriginalRoll> sources, boolean onSite) {
        if (!onSite) {
            validateStandardWidth(finish, dto);
            return;
        }
        if (dto.getFinishWidth() == null || dto.getFinishWidth() <= 0) {
            throw new BusinessException("现场定尺成品门幅必须大于0：" + finish.getFinishRollNo());
        }
        int sourceWidth = minimumSourceWidth(sources);
        if (dto.getFinishWidth() > sourceWidth) {
            throw new BusinessException("成品门幅不能超过来源母卷门幅 " + sourceWidth + "mm："
                    + finish.getFinishRollNo());
        }
    }

    private static void validateStandardWidth(FinishRoll finish, BackRecordFinishDTO dto) {
        if (finish.getFinishWidth() == null || finish.getFinishWidth() <= 0) {
            throw new BusinessException("标准加工成品门幅无效，请回退待下发重配：" + finish.getFinishRollNo());
        }
        if (dto.getFinishWidth() != null && !dto.getFinishWidth().equals(finish.getFinishWidth())) {
            throw new BusinessException("标准加工成品门幅不可在回录中修改：" + finish.getFinishRollNo());
        }
        if (dto.getFinishDiameter() != null && !dto.getFinishDiameter().equals(finish.getFinishDiameter())) {
            throw new BusinessException("标准加工成品直径不可在回录中修改：" + finish.getFinishRollNo());
        }
        if (dto.getFinishCoreDiameter() != null
                && !dto.getFinishCoreDiameter().equals(finish.getFinishCoreDiameter())) {
            throw new BusinessException("标准加工成品纸芯不可在回录中修改：" + finish.getFinishRollNo());
        }
    }

    private static int minimumSourceWidth(List<OriginalRoll> sources) {
        return sources.stream().mapToInt(BackRecordFinishRules::effectiveWidth).min()
                .orElseThrow(() -> new BusinessException("来源母卷门幅无效，无法提交现场定尺回录"));
    }

    private static int effectiveWidth(OriginalRoll roll) {
        if (roll.getActualWidth() != null && roll.getActualWidth() > 0) {
            return roll.getActualWidth();
        }
        return roll.getOriginalWidth() == null ? 0 : roll.getOriginalWidth();
    }

    private static boolean isOnSite(OriginalRoll roll) {
        return Integer.valueOf(PROCESS_MODE_ON_SITE).equals(roll.getProcessMode());
    }
}

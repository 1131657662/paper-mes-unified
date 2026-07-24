package com.paper.mes.processorder.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.calc.FeeCalculator;

public final class ProcessModePolicy {

    public static final int STANDARD = 1;
    public static final int ON_SITE = 2;
    public static final int DIRECT_SHIP = 3;
    public static final int SERVICE_ONLY = 4;

    private ProcessModePolicy() {
    }

    public static void requireValid(Integer processMode, Integer mainStepType) {
        int mode = processMode == null ? STANDARD : processMode;
        if (mode < STANDARD || mode > SERVICE_ONLY) {
            throw new BusinessException("加工方式不正确");
        }
        if (!requiresMainProcess(mode)) return;
        if (mainStepType == null) {
            throw new BusinessException("标准加工和现场定尺必须选择主工艺");
        }
        if (mainStepType != FeeCalculator.STEP_TYPE_SAW
                && mainStepType != FeeCalculator.STEP_TYPE_REWIND) {
            throw new BusinessException("主工艺类型只能是锯纸或复卷");
        }
    }

    public static boolean requiresMainProcess(Integer processMode) {
        return !isDirectShip(processMode) && !isServiceOnly(processMode);
    }

    public static boolean isDirectShip(Integer processMode) {
        return Integer.valueOf(DIRECT_SHIP).equals(processMode);
    }

    public static boolean isServiceOnly(Integer processMode) {
        return Integer.valueOf(SERVICE_ONLY).equals(processMode);
    }

    public static boolean supportsServiceSteps(Integer processMode) {
        return Integer.valueOf(STANDARD).equals(processMode)
                || Integer.valueOf(ON_SITE).equals(processMode)
                || Integer.valueOf(SERVICE_ONLY).equals(processMode);
    }
}

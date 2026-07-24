package com.paper.mes.system.config.controller;

import com.paper.mes.common.R;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import com.paper.mes.system.config.dto.RuntimeConfigVO;
import com.paper.mes.system.config.dto.RuntimeDictOptionVO;
import com.paper.mes.system.config.service.SystemConfigService;
import com.paper.mes.system.config.service.SystemDictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/system/runtime")
@RequiredArgsConstructor
public class SystemRuntimeConfigController {

    private static final int MAX_KEYS = 20;
    private static final Set<String> PUBLIC_CONFIG_KEYS = Set.of(
            "ui.defaultPageSize",
            "print.processOrderTitle",
            "process.autoFinishConfig",
            "process.spareRollNoCount",
            "process.weightTolerancePercent",
            "process.pricingAutoApproveLimit",
            "settle.discountAutoApproveLimit",
            "settle.discountMaxAmount",
            "settle.discountMaxPercent"
    );

    private final SystemDictService systemDictService;
    private final SystemConfigService systemConfigService;

    @GetMapping("/dict-options")
    public R<List<RuntimeDictOptionVO>> dictOptions(@RequestParam String types) {
        List<RuntimeDictOptionVO> options = systemDictService.enabledByTypes(split(types))
                .stream()
                .map(RuntimeDictOptionVO::from)
                .toList();
        return R.success(options);
    }

    @GetMapping("/configs")
    public R<List<RuntimeConfigVO>> configs(@RequestParam String keys) {
        List<RuntimeConfigVO> configs = systemConfigService.enabledByKeys(publicConfigKeys(keys))
                .stream()
                .map(RuntimeConfigVO::from)
                .toList();
        return R.success(configs);
    }

    private List<String> split(String value) {
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private List<String> publicConfigKeys(String value) {
        List<String> keys = split(value);
        if (keys.size() > MAX_KEYS || !PUBLIC_CONFIG_KEYS.containsAll(keys)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "包含未公开的运行时配置键");
        }
        return keys;
    }
}

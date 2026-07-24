package com.paper.mes.processorder.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ErrorCode;
import com.paper.mes.processorder.dto.PrintViewVersion;
import com.paper.mes.processorder.dto.ProcessOrderDetailVO;
import com.paper.mes.processorder.dto.ProcessOrderPrintViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** 加工单打印版本读取与旧快照兼容。 */
@Slf4j
final class ProcessOrderPrintViewReader {

    private ProcessOrderPrintViewReader() {
    }

    static ProcessOrderPrintViewVO read(ProcessOrderDetailVO live, PrintViewVersion version,
                                        ObjectMapper objectMapper) {
        String snapshot = version == PrintViewVersion.FINISHED
                ? live.getOrder().getSnapFinish() : live.getOrder().getSnapPrint();
        if (!StringUtils.hasText(snapshot)) {
            return readWithoutSnapshot(live, version, objectMapper);
        }
        try {
            JsonNode root = objectMapper.readTree(snapshot);
            validateRoot(root, version);
            ProcessOrderDetailVO stored = ProcessOrderSnapshotDetailCodec.read(root, objectMapper);
            boolean legacy = stored == null;
            ProcessOrderDetailVO detail = legacy
                    ? legacyDetail(live, root, version, objectMapper) : stored;
            return view(live, version, root, detail, legacy);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("加工单打印快照解析失败：{}", ex.getMessage(), ex);
            throw corruptedSnapshot();
        }
    }

    private static ProcessOrderPrintViewVO readWithoutSnapshot(ProcessOrderDetailVO live,
                                                               PrintViewVersion version,
                                                               ObjectMapper objectMapper) {
        if (version == PrintViewVersion.FINISHED) {
            throw new BusinessException(ErrorCode.E002, "完成快照尚未生成");
        }
        if (live.getOrder().getOrderStatus() != null && live.getOrder().getOrderStatus() >= 2) {
            throw corruptedSnapshot();
        }
        ProcessOrderDetailVO detail = ProcessOrderSnapshotDetailCodec.copy(live, objectMapper);
        ProcessOrderPrintDetailPolicy.filter(detail);
        detail.getOrder().setSnapPrint(null);
        detail.getOrder().setSnapFinish(null);
        ProcessOrderPrintViewVO result = baseView(live, version);
        result.setSource("LIVE_PREVIEW");
        result.setDetail(detail);
        return result;
    }

    private static ProcessOrderPrintViewVO view(ProcessOrderDetailVO live, PrintViewVersion version,
                                                JsonNode root, ProcessOrderDetailVO detail,
                                                boolean legacy) {
        ProcessOrderPrintViewVO result = baseView(live, version);
        result.setSource(legacy ? "LEGACY_FALLBACK" : "SNAPSHOT");
        result.setSchemaVersion(text(root, "schema_version"));
        result.setSnapshotTime(text(root, version == PrintViewVersion.ISSUED
                ? "print_time" : "back_record_time"));
        result.setSnapshotUser(text(root, version == PrintViewVersion.ISSUED
                ? "print_user" : "back_record_user"));
        if (legacy) {
            result.setWarning("该历史快照生成于完整打印详情上线前，已按冻结字段还原，缺失结构使用受保护的当前单据补齐。");
        }
        result.setDetail(detail);
        return result;
    }

    private static ProcessOrderPrintViewVO baseView(ProcessOrderDetailVO live,
                                                    PrintViewVersion version) {
        ProcessOrderPrintViewVO result = new ProcessOrderPrintViewVO();
        result.setVersion(version);
        List<PrintViewVersion> versions = new ArrayList<>();
        versions.add(PrintViewVersion.ISSUED);
        if (StringUtils.hasText(live.getOrder().getSnapFinish())) {
            versions.add(PrintViewVersion.FINISHED);
        }
        result.setAvailableVersions(versions);
        return result;
    }

    private static ProcessOrderDetailVO legacyDetail(ProcessOrderDetailVO live, JsonNode root,
                                                      PrintViewVersion version,
                                                      ObjectMapper objectMapper) {
        return version == PrintViewVersion.ISSUED
                ? LegacyProcessOrderSnapshotOverlay.issued(live, root, objectMapper)
                : LegacyProcessOrderSnapshotOverlay.finished(live, root, objectMapper);
    }

    private static void validateRoot(JsonNode root, PrintViewVersion version) {
        if (root == null || !root.isObject() || !root.path("original_rolls").isArray()
                || !root.path("finish_rolls").isArray()) {
            throw corruptedSnapshot();
        }
        String requiredTime = version == PrintViewVersion.ISSUED ? "print_time" : "back_record_time";
        if (!StringUtils.hasText(text(root, "schema_version"))
                || !StringUtils.hasText(text(root, requiredTime))) {
            throw corruptedSnapshot();
        }
    }

    private static String text(JsonNode root, String key) {
        JsonNode value = root.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static BusinessException corruptedSnapshot() {
        return new BusinessException(ErrorCode.E008,
                "加工单历史快照损坏，已禁止使用当前业务数据替代，请联系管理员处理");
    }
}

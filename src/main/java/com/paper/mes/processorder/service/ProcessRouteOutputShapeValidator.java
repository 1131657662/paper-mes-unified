package com.paper.mes.processorder.service;
import com.paper.mes.common.BusinessException;
import com.paper.mes.processorder.dto.ProcessRoutePreviewDTO;
import com.paper.mes.processorder.dto.ProcessRoutePreviewVO;
import com.paper.mes.processorder.dto.RewindLayoutItemPlanDTO;
import com.paper.mes.processorder.dto.RewindSegmentPlanDTO;
import com.paper.mes.processorder.dto.RewindSourcePlanDTO;
import com.paper.mes.processorder.model.WidthDifferencePolicy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/** Ensures client output rows match the physical route layout expansion. */
final class ProcessRouteOutputShapeValidator {
    private ProcessRouteOutputShapeValidator() {
    }
    static void validate(ProcessRoutePreviewDTO.RouteStageDTO stage,
                         List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs,
                         Integer sourceWidth,
                         WidthDifferencePolicy policy,
                         Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs) {
        if (stage.getPlan() == null) return;
        ShapePlan expected = new ShapePlan();
        if (stage.getPlan().getFinishSpecs() != null && !stage.getPlan().getFinishSpecs().isEmpty()) {
            for (var spec : stage.getPlan().getFinishSpecs()) {
                int count = positive(spec.getCount());
                if (isTrim(spec.getItemType())) {
                    expected.explicitTrimWidth += width(spec.getFinishWidth()) * count;
                } else {
                    expected.add(new Shape(width(spec.getFinishWidth()), 0,
                            spec.getFinishDiameter(), spec.getFinishCoreDiameter()), count);
                }
            }
        } else if (stage.getPlan().getSegments() != null && !stage.getPlan().getSegments().isEmpty()) {
            Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios =
                    ProcessRouteSegmentRatioResolver.effectiveRatios(stage.getPlan().getSegments());
            for (var segment : stage.getPlan().getSegments()) {
                int repeat = positive(segment.getRepeatCount());
                for (RewindLayoutItemPlanDTO item : safe(segment.getLayoutItems())) {
                    int count = positive(item.getQuantity()) * repeat;
                    if (isTrim(item.getItemType())) {
                        expected.explicitTrimWidth += width(item.getWidth()) * count;
                    } else {
                        expected.add(new Shape(width(item.getWidth()), 0,
                                expectedDiameter(segment, item), expectedCoreDiameter(segment, item)), count);
                    }
                }
                expected.segmentTrimWidths.add(segmentTrimWidth(stage, segment, sourceWidth, policy,
                        inputs, effectiveRatios));
            }
        }
        if (expected.shapes.isEmpty()) return;
        int trimWidth = expected.trimWidth(sourceWidth, policy);
        if (trimWidth > 0) expected.add(new Shape(trimWidth, 1, null, null), 1);
        List<Shape> actual = expand(outputs);
        if (actual.size() != expected.shapes.size()) {
            throw new BusinessException("阶段产物数量必须与工艺排版展开数量一致");
        }
        for (int index = 0; index < actual.size(); index++) {
            Shape planned = expected.shapes.get(index);
            Shape received = actual.get(index);
            if (planned.isRemain() != received.isRemain()
                    || (planned.width() > 0 && planned.width() != received.width())
                    || !sameOptional(planned.finishDiameter(), received.finishDiameter())
                    || !sameOptional(planned.finishCoreDiameter(), received.finishCoreDiameter())) {
                throw new BusinessException("阶段产物规格必须与工艺排版一致");
            }
        }
    }
    private static List<Shape> expand(List<ProcessRoutePreviewDTO.RouteOutputDTO> outputs) {
        List<Shape> result = new ArrayList<>();
        for (var output : outputs) {
            for (int index = 0; index < positive(output.getCount()); index++) {
                result.add(new Shape(width(output.getFinishWidth()),
                        output.getIsRemain() != null && output.getIsRemain() == 1 ? 1 : 0,
                        output.getFinishDiameter(), output.getFinishCoreDiameter()));
            }
        }
        return result;
    }
    private static BigDecimal segmentTrimWidth(ProcessRoutePreviewDTO.RouteStageDTO stage,
                                        RewindSegmentPlanDTO segment,
                                        Integer sourceWidth,
                                        WidthDifferencePolicy policy,
                                        Map<String, ProcessRoutePreviewVO.RouteOutputVO> inputs,
                                        Map<RewindSourcePlanDTO, BigDecimal> effectiveRatios) {
        if (sourceWidth == null || sourceWidth <= 0) return BigDecimal.ZERO;
        int finishWidth = safe(segment.getLayoutItems()).stream()
                .filter(item -> !isTrim(item.getItemType()))
                .mapToInt(item -> width(item.getWidth()) * positive(item.getQuantity()))
                .sum();
        int explicitTrim = safe(segment.getLayoutItems()).stream()
                .filter(item -> isTrim(item.getItemType()))
                .mapToInt(item -> width(item.getWidth()) * positive(item.getQuantity()))
                .sum();
        int difference = Math.max(0, sourceWidth - finishWidth - explicitTrim);
        BigDecimal ratio = ProcessRouteSegmentRatioResolver.segmentRatio(
                stage, segment, inputs, effectiveRatios);
        if (policy == WidthDifferencePolicy.REMAINDER) {
            return ratio.multiply(BigDecimal.valueOf(explicitTrim + difference));
        }
        return ratio.multiply(BigDecimal.valueOf(explicitTrim));
    }
    private static int rounded(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).intValue();
    }
    private static Integer expectedDiameter(RewindSegmentPlanDTO segment, RewindLayoutItemPlanDTO item) {
        if (segment.getTargetDiameter() != null) return segment.getTargetDiameter();
        return safeLayers(item).stream().map(layer -> layer.getOutDiameter())
                .filter(value -> value != null).max(Integer::compareTo).orElse(null);
    }
    private static Integer expectedCoreDiameter(RewindSegmentPlanDTO segment, RewindLayoutItemPlanDTO item) {
        if (segment.getFinishCoreDiameter() != null) return segment.getFinishCoreDiameter();
        return safeLayers(item).stream().map(layer -> layer.getCoreDiameter())
                .filter(value -> value != null).findFirst().orElse(null);
    }
    private static List<com.paper.mes.processorder.dto.FinishConfigSpecDTO.FinishLayerDTO> safeLayers(
            RewindLayoutItemPlanDTO item) {
        return item.getLayers() == null ? List.of() : item.getLayers();
    }
    private static boolean sameOptional(Integer expected, Integer actual) {
        return expected == null || expected.equals(actual);
    }
    private static boolean isTrim(String itemType) {
        return "TRIM".equalsIgnoreCase(itemType);
    }
    private static int width(Integer value) {
        if (value == null || value <= 0) {
            throw new BusinessException("工艺排版门幅必须大于0");
        }
        return value;
    }
    private static int positive(Integer value) {
        return value == null ? 1 : Math.max(1, value);
    }
    private static List<RewindLayoutItemPlanDTO> safe(List<RewindLayoutItemPlanDTO> value) {
        return value == null ? List.of() : value;
    }
    private static final class ShapePlan {
        private final List<Shape> shapes = new ArrayList<>();
        private final List<BigDecimal> segmentTrimWidths = new ArrayList<>();
        private int explicitTrimWidth;
        private void add(Shape shape, int count) {
            for (int index = 0; index < count; index++) shapes.add(shape);
        }
        private int trimWidth(Integer sourceWidth, WidthDifferencePolicy policy) {
            if (!segmentTrimWidths.isEmpty()) {
                return rounded(segmentTrimWidths.stream().reduce(BigDecimal.ZERO, BigDecimal::add));
            }
            int difference = sourceWidth == null ? 0 : Math.max(0, sourceWidth - finishWidth() - explicitTrimWidth);
            return explicitTrimWidth + (policy == WidthDifferencePolicy.REMAINDER ? difference : 0);
        }
        private int finishWidth() {
            return shapes.stream().mapToInt(shape -> shape.isRemain == 0 ? shape.width : 0).sum();
        }
    }
    private record Shape(int width, int isRemain, Integer finishDiameter, Integer finishCoreDiameter) {
    }
}

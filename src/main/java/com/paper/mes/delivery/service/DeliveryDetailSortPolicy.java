package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

/** Keeps detail-table sorting rules identical for API exports and future server-side paging. */
public final class DeliveryDetailSortPolicy {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "orderNo", "finishRollNo", "paperName", "gramWeight", "spec", "actualWeight",
            "outWeight", "remainingWeight", "originalSummary", "remark", "actualRemark");

    private DeliveryDetailSortPolicy() {
    }

    public static List<DeliverySortSpec> normalize(List<DeliverySortSpec> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        List<DeliverySortSpec> normalized = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DeliverySortSpec item : requested) {
            if (item == null || !ALLOWED_FIELDS.contains(item.field())
                    || !("asc".equals(item.direction()) || "desc".equals(item.direction()))) {
                throw new BusinessException("出库单排序字段或方向不受支持");
            }
            if (seen.add(item.field())) normalized.add(item);
        }
        return List.copyOf(normalized);
    }

    public static List<DeliveryDetailItemVO> sort(
            List<DeliveryDetailItemVO> details, List<DeliverySortSpec> requested) {
        List<DeliverySortSpec> sorts = normalize(requested);
        if (details == null || details.size() < 2 || sorts.isEmpty()) return details == null ? List.of() : details;
        return IntStream.range(0, details.size())
                .mapToObj(index -> new IndexedItem(details.get(index), index))
                .sorted((left, right) -> compare(left, right, sorts))
                .map(IndexedItem::item)
                .toList();
    }

    private static int compare(IndexedItem left, IndexedItem right, List<DeliverySortSpec> sorts) {
        for (DeliverySortSpec sort : sorts) {
            Object leftValue = value(left.item(), sort.field());
            Object rightValue = value(right.item(), sort.field());
            int result = compareValue(leftValue, rightValue);
            if (isBlank(leftValue) || isBlank(rightValue)) {
                if (result != 0) return result;
                continue;
            }
            if (result != 0) return "asc".equals(sort.direction()) ? result : -result;
        }
        return Integer.compare(left.index(), right.index());
    }

    private static Object value(DeliveryDetailItemVO item, String field) {
        return switch (field) {
            case "orderNo" -> item.getOrderNo();
            case "finishRollNo" -> item.getFinishRollNo();
            case "paperName" -> item.getPaperName();
            case "gramWeight" -> item.getGramWeight();
            case "spec" -> item.getFinishWidth();
            case "actualWeight" -> item.getActualWeight();
            case "outWeight" -> item.getOutWeight();
            case "remainingWeight" -> item.getRemainingWeight();
            case "originalSummary" -> item.getOriginalSummary() == null
                    ? item.getOriginalRollNos() : item.getOriginalSummary();
            case "remark" -> item.getRemark();
            case "actualRemark" -> item.getActualRemark();
            default -> null;
        };
    }

    private static int compareValue(Object left, Object right) {
        if (isBlank(left)) return isBlank(right) ? 0 : 1;
        if (isBlank(right)) return -1;
        if (left instanceof Number || right instanceof Number) {
            return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString()));
        }
        return naturalCompare(left.toString(), right.toString());
    }

    private static boolean isBlank(Object value) {
        return value == null || value instanceof String text && text.isBlank();
    }

    private static int naturalCompare(String left, String right) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            boolean leftDigit = asciiDigit(left.charAt(leftIndex));
            boolean rightDigit = asciiDigit(right.charAt(rightIndex));
            int leftEnd = tokenEnd(left, leftIndex, leftDigit);
            int rightEnd = tokenEnd(right, rightIndex, rightDigit);
            String leftPart = left.substring(leftIndex, leftEnd);
            String rightPart = right.substring(rightIndex, rightEnd);
            int result = leftDigit && rightDigit
                    ? compareNumericText(leftPart, rightPart)
                    : leftPart.compareTo(rightPart);
            if (result != 0) return result;
            leftIndex = leftEnd;
            rightIndex = rightEnd;
        }
        return Integer.compare(left.length() - leftIndex, right.length() - rightIndex);
    }

    private static int tokenEnd(String value, int start, boolean digit) {
        int index = start;
        while (index < value.length() && asciiDigit(value.charAt(index)) == digit) index++;
        return index;
    }

    private static boolean asciiDigit(char value) {
        return value >= '0' && value <= '9';
    }

    private static int compareNumericText(String left, String right) {
        String normalizedLeft = left.replaceFirst("^0+(?=\\d)", "");
        String normalizedRight = right.replaceFirst("^0+(?=\\d)", "");
        if (normalizedLeft.length() != normalizedRight.length()) {
            return Integer.compare(normalizedLeft.length(), normalizedRight.length());
        }
        return normalizedLeft.compareTo(normalizedRight);
    }

    private record IndexedItem(DeliveryDetailItemVO item, int index) {
    }
}

package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.dto.DeliveryCustomerSpecVO;
import com.paper.mes.delivery.dto.DeliveryDetailItemVO;
import com.paper.mes.delivery.dto.DeliverySortSpec;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/** Keeps customer and trace table ordering stable with the browser table. */
public final class DeliveryCustomerSortPolicy {
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "finishRollNo", "customerPaperName", "customerSpecification", "customerDisplayWeight",
            "orderNo", "customerRemark", "sourceMotherRoll");

    private DeliveryCustomerSortPolicy() {
    }

    public static List<DeliverySortSpec> normalize(List<DeliverySortSpec> requested) {
        if (requested == null || requested.isEmpty()) return List.of();
        List<DeliverySortSpec> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (DeliverySortSpec item : requested) {
            if (item == null || !ALLOWED_FIELDS.contains(item.field())
                    || !("asc".equals(item.direction()) || "desc".equals(item.direction()))) {
                throw new BusinessException("客户单据排序字段或方向不受支持");
            }
            if (seen.add(item.field())) result.add(item);
        }
        return List.copyOf(result);
    }

    public static List<DeliveryCustomerSpecVO> sort(
            List<DeliveryCustomerSpecVO> specs, List<DeliveryDetailItemVO> details,
            List<DeliverySortSpec> requested) {
        List<DeliverySortSpec> sorts = normalize(requested);
        if (specs == null || specs.size() < 2 || sorts.isEmpty()) return specs == null ? List.of() : specs;
        Map<String, DeliveryDetailItemVO> detailByUuid = detailMap(details);
        return IntStream.range(0, specs.size())
                .mapToObj(index -> new IndexedItem(specs.get(index), index))
                .sorted((left, right) -> compare(left, right, sorts, detailByUuid))
                .map(IndexedItem::item)
                .toList();
    }

    private static Map<String, DeliveryDetailItemVO> detailMap(List<DeliveryDetailItemVO> details) {
        Map<String, DeliveryDetailItemVO> result = new HashMap<>();
        if (details != null) for (DeliveryDetailItemVO detail : details) {
            if (detail.getUuid() != null) result.put(detail.getUuid(), detail);
            if (detail.getFinishUuid() != null) result.put("finish:" + detail.getFinishUuid(), detail);
            if (detail.getOrderNo() != null && detail.getFinishRollNo() != null) {
                result.put("roll:" + detail.getOrderNo() + '|' + detail.getFinishRollNo(), detail);
            }
        }
        return result;
    }

    private static int compare(IndexedItem left, IndexedItem right, List<DeliverySortSpec> sorts,
                               Map<String, DeliveryDetailItemVO> detailByUuid) {
        for (DeliverySortSpec sort : sorts) {
            Object leftValue = value(left.item(), sort.field(), detailByUuid);
            Object rightValue = value(right.item(), sort.field(), detailByUuid);
            int result = compareValue(leftValue, rightValue);
            if (blank(leftValue) || blank(rightValue)) {
                if (result != 0) return result;
                continue;
            }
            if (result != 0) return "asc".equals(sort.direction()) ? result : -result;
        }
        return Integer.compare(left.index(), right.index());
    }

    private static Object value(DeliveryCustomerSpecVO item, String field,
                                Map<String, DeliveryDetailItemVO> detailByUuid) {
        return switch (field) {
            case "finishRollNo" -> item.getFinishRollNo();
            case "customerPaperName" -> item.getCustomerPaperName();
            case "customerSpecification" -> specification(item);
            case "customerDisplayWeight" -> item.getCustomerDisplayWeight();
            case "orderNo" -> item.getOrderNo();
            case "customerRemark" -> item.getCustomerRemark();
            case "sourceMotherRoll" -> sourceMotherRoll(item, detailByUuid);
            default -> null;
        };
    }

    private static String specification(DeliveryCustomerSpecVO item) {
        String gram = item.getCustomerGramWeight() == null ? "" : item.getCustomerGramWeight().toString();
        String width = item.getCustomerFinishWidth() == null ? "" : item.getCustomerFinishWidth().toString();
        return gram.isEmpty() && width.isEmpty() ? null : gram + "/" + width;
    }

    private static String sourceMotherRoll(DeliveryCustomerSpecVO item,
                                           Map<String, DeliveryDetailItemVO> detailByUuid) {
        DeliveryDetailItemVO detail = item.getDeliveryDetailUuid() == null
                ? null : detailByUuid.get(item.getDeliveryDetailUuid());
        if (detail == null && item.getFinishUuid() != null) {
            detail = detailByUuid.get("finish:" + item.getFinishUuid());
        }
        if (detail == null && item.getOrderNo() != null && item.getFinishRollNo() != null) {
            detail = detailByUuid.get("roll:" + item.getOrderNo() + '|' + item.getFinishRollNo());
        }
        if (detail == null) return null;
        return detail.getOriginalSummary() == null ? detail.getOriginalRollNos() : detail.getOriginalSummary();
    }

    private static int compareValue(Object left, Object right) {
        if (blank(left)) return blank(right) ? 0 : 1;
        if (blank(right)) return -1;
        if (left instanceof Number || right instanceof Number) {
            return new BigDecimal(left.toString()).compareTo(new BigDecimal(right.toString()));
        }
        return naturalCompare(left.toString(), right.toString());
    }

    private static boolean blank(Object value) {
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
            int result = leftDigit && rightDigit ? numericCompare(leftPart, rightPart) : leftPart.compareTo(rightPart);
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

    private static int numericCompare(String left, String right) {
        String normalizedLeft = left.replaceFirst("^0+(?=\\d)", "");
        String normalizedRight = right.replaceFirst("^0+(?=\\d)", "");
        if (normalizedLeft.length() != normalizedRight.length()) {
            return Integer.compare(normalizedLeft.length(), normalizedRight.length());
        }
        return normalizedLeft.compareTo(normalizedRight);
    }

    private record IndexedItem(DeliveryCustomerSpecVO item, int index) {
    }
}

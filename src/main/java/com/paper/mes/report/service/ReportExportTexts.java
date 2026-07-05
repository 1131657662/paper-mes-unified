package com.paper.mes.report.service;

import com.paper.mes.report.dto.ReportDimensionVO;

final class ReportExportTexts {

    private ReportExportTexts() {
    }

    static String label(ReportDimensionVO item, String dimension) {
        if ("status".equals(dimension)) {
            return statusText(parseInt(item.getDimensionKey()));
        }
        if ("invoice".equals(dimension)) {
            return "1".equals(item.getDimensionKey()) ? "开票" : "不开票";
        }
        if ("settleType".equals(dimension)) {
            return "1".equals(item.getDimensionKey()) ? "次结" : "月结";
        }
        return text(item.getDimensionName());
    }

    static String statusText(Integer status) {
        if (status == null) {
            return "-";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "待下发";
            case 2 -> "加工中";
            case 3 -> "待回录";
            case 4 -> "已完成";
            case 5 -> "已结算";
            case 6 -> "已作废";
            default -> String.valueOf(status);
        };
    }

    static String settleText(Integer value) {
        return value == null ? "-" : value == 2 ? "月结" : value == 1 ? "次结" : String.valueOf(value);
    }

    static String invoiceText(Integer value) {
        return value == null ? "-" : value == 1 ? "开票" : "不开票";
    }

    private static Integer parseInt(String value) {
        try {
            return value == null ? null : Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(Object value) {
        return value == null ? "-" : value.toString();
    }
}

package com.paper.mes.processorder.calc;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/** Allocates rewind estimates as whole kilograms while preserving the source total. */
final class RewindEstimateWeightAllocator {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final int ACTUAL_SCALE = 3;

    private RewindEstimateWeightAllocator() {
    }

    static List<RewindWeightCalculator.PieceResult> allocate(
            BigDecimal sourceWeight,
            List<RewindWeightCalculator.PieceInput> pieces,
            BigDecimal trimTotalWidth,
            BigDecimal originalWidth,
            BigDecimal totalLoss) {
        if (pieces.isEmpty()) return List.of();
        requirePositiveSourceWeight(sourceWeight);
        return hasMeasuredPiece(pieces)
                ? allocateWithMeasuredPieces(sourceWeight, pieces, trimTotalWidth, originalWidth, totalLoss)
                : allocateIntegerEstimates(sourceWeight, pieces, trimTotalWidth, originalWidth, totalLoss);
    }

    private static List<RewindWeightCalculator.PieceResult> allocateIntegerEstimates(
            BigDecimal sourceWeight,
            List<RewindWeightCalculator.PieceInput> pieces,
            BigDecimal trimTotalWidth,
            BigDecimal originalWidth,
            BigDecimal totalLoss) {
        BigDecimal total = IntegerWeightAllocator.roundTotal(sourceWeight);
        BigDecimal trimTotal = IntegerWeightAllocator.roundTotal(trimTotal(sourceWeight, trimTotalWidth, originalWidth));
        BigDecimal loss = IntegerWeightAllocator.roundTotal(value(totalLoss));
        BigDecimal outputTotal = total.subtract(trimTotal).subtract(loss);
        requireNonNegative(outputTotal);

        List<BigDecimal> weights = IntegerWeightAllocator.allocate(outputTotal,
                pieces.stream().map(piece -> piece.areaBasis).toList());
        List<BigDecimal> trimShares = IntegerWeightAllocator.allocate(trimTotal,
                pieces.stream().map(piece -> BigDecimal.ONE).toList());
        return results(weights, trimShares);
    }

    private static List<RewindWeightCalculator.PieceResult> allocateWithMeasuredPieces(
            BigDecimal sourceWeight,
            List<RewindWeightCalculator.PieceInput> pieces,
            BigDecimal trimTotalWidth,
            BigDecimal originalWidth,
            BigDecimal totalLoss) {
        BigDecimal total = IntegerWeightAllocator.roundTotal(value(sourceWeight));
        BigDecimal trimTotal = IntegerWeightAllocator.roundTotal(trimTotal(sourceWeight, trimTotalWidth, originalWidth));
        BigDecimal loss = IntegerWeightAllocator.roundTotal(value(totalLoss));
        BigDecimal measuredTotal = measuredTotal(pieces);
        BigDecimal distributable = total.subtract(measuredTotal).subtract(loss).subtract(trimTotal);
        int lastUnmeasured = lastUnmeasuredIndex(pieces);
        requireMeasuredClosure(distributable, lastUnmeasured);

        List<BigDecimal> estimatedWeights = allocateUnmeasuredIntegerWeights(distributable, pieces,
                lastUnmeasured);
        List<BigDecimal> trimShares = IntegerWeightAllocator.allocate(trimTotal,
                java.util.Collections.nCopies(pieces.size(), BigDecimal.ONE));
        List<RewindWeightCalculator.PieceResult> result = new ArrayList<>(pieces.size());
        for (int index = 0; index < pieces.size(); index++) {
            RewindWeightCalculator.PieceInput piece = pieces.get(index);
            BigDecimal weight = isMeasured(piece) ? roundedActual(piece.actualWeight) : estimatedWeights.get(index);
            result.add(new RewindWeightCalculator.PieceResult(weight, trimShares.get(index)));
        }
        return result;
    }

    private static List<BigDecimal> allocateUnmeasuredIntegerWeights(BigDecimal distributable,
                                                                       List<RewindWeightCalculator.PieceInput> pieces,
                                                                       int lastUnmeasured) {
        if (lastUnmeasured < 0) return java.util.Collections.nCopies(pieces.size(), BigDecimal.ZERO.setScale(0));
        if (distributable.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("实称重量与原纸重量相减后不是整数，无法生成整数预估重量");
        }
        BigDecimal estimatedTotal = distributable.setScale(0, RoundingMode.UNNECESSARY);
        List<BigDecimal> bases = pieces.stream()
                .map(piece -> isMeasured(piece) ? BigDecimal.ZERO : piece.areaBasis)
                .toList();
        return IntegerWeightAllocator.allocate(estimatedTotal, bases);
    }

    private static boolean hasMeasuredPiece(List<RewindWeightCalculator.PieceInput> pieces) {
        return pieces.stream().anyMatch(RewindEstimateWeightAllocator::isMeasured);
    }

    private static BigDecimal trimTotal(BigDecimal total, BigDecimal trimWidth, BigDecimal sourceWidth) {
        if (trimWidth == null || trimWidth.signum() <= 0 || sourceWidth == null || sourceWidth.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return value(total).multiply(trimWidth, MC).divide(sourceWidth, MC);
    }

    private static BigDecimal measuredTotal(List<RewindWeightCalculator.PieceInput> pieces) {
        return pieces.stream().filter(RewindEstimateWeightAllocator::isMeasured)
                .map(piece -> roundedActual(piece.actualWeight)).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static int lastUnmeasuredIndex(List<RewindWeightCalculator.PieceInput> pieces) {
        int result = -1;
        for (int index = 0; index < pieces.size(); index++) {
            if (!isMeasured(pieces.get(index))) result = index;
        }
        return result;
    }

    private static List<RewindWeightCalculator.PieceResult> results(List<BigDecimal> weights,
                                                                       List<BigDecimal> trimShares) {
        List<RewindWeightCalculator.PieceResult> result = new ArrayList<>(weights.size());
        for (int index = 0; index < weights.size(); index++) {
            result.add(new RewindWeightCalculator.PieceResult(weights.get(index), trimShares.get(index)));
        }
        return result;
    }

    private static void requireNonNegative(BigDecimal outputTotal) {
        if (outputTotal.signum() < 0) {
            throw new IllegalArgumentException("预估损耗与修边重量合计不能超过来源重量");
        }
    }

    private static void requirePositiveSourceWeight(BigDecimal sourceWeight) {
        if (sourceWeight == null || sourceWeight.signum() <= 0) {
            throw new IllegalArgumentException("来源母卷重量未知或不大于0，不能生成预估重量");
        }
    }

    private static void requireMeasuredClosure(BigDecimal distributable, int lastUnmeasured) {
        if (distributable.signum() < 0) {
            throw new IllegalArgumentException("实称重量、损耗与修边重量合计不能超过原纸实际总重");
        }
        if (lastUnmeasured < 0 && distributable.setScale(ACTUAL_SCALE, RoundingMode.HALF_UP).signum() != 0) {
            throw new IllegalArgumentException("全部成品均已实称时，重量合计必须与原纸实际总重闭合");
        }
    }

    private static boolean isMeasured(RewindWeightCalculator.PieceInput piece) {
        return piece.actualWeight != null && piece.actualWeight.signum() > 0;
    }

    private static BigDecimal roundedActual(BigDecimal value) {
        return value.setScale(ACTUAL_SCALE, RoundingMode.HALF_UP);
    }

    private static BigDecimal value(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

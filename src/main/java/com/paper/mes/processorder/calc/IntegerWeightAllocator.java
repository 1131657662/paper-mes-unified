package com.paper.mes.processorder.calc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Allocates estimated weights as integer kilograms while preserving the total. */
public final class IntegerWeightAllocator {

    private IntegerWeightAllocator() {
    }

    public static BigDecimal roundTotal(BigDecimal value) {
        return value == null ? null : value.setScale(0, RoundingMode.HALF_UP);
    }

    public static List<BigDecimal> allocate(BigDecimal total, List<BigDecimal> bases) {
        if (bases == null || bases.isEmpty()) return List.of();
        if (total != null && total.signum() < 0) {
            throw new IllegalArgumentException("预估重量分配总重不能为负数");
        }
        BigDecimal roundedTotal = roundTotal(total);
        if (roundedTotal == null || roundedTotal.signum() <= 0) {
            return zeros(bases.size());
        }
        BigDecimal basisTotal = bases.stream()
                .map(IntegerWeightAllocator::positive)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (basisTotal.signum() == 0) {
            return allocateEvenly(roundedTotal.toBigIntegerExact(), bases.size());
        }
        return allocateByBasis(roundedTotal.toBigIntegerExact(), bases, basisTotal);
    }

    private static List<BigDecimal> allocateByBasis(BigInteger total,
                                                     List<BigDecimal> bases,
                                                     BigDecimal basisTotal) {
        List<BigInteger> floors = new ArrayList<>(bases.size());
        List<Share> shares = new ArrayList<>(bases.size());
        BigInteger allocated = BigInteger.ZERO;
        for (int index = 0; index < bases.size(); index++) {
            BigDecimal raw = new BigDecimal(total).multiply(positive(bases.get(index)))
                    .divide(basisTotal, 12, RoundingMode.HALF_UP);
            BigInteger floor = raw.setScale(0, RoundingMode.DOWN).toBigIntegerExact();
            floors.add(floor);
            allocated = allocated.add(floor);
            shares.add(new Share(index, raw.subtract(new BigDecimal(floor))));
        }
        long remainder = total.subtract(allocated).longValueExact();
        shares.sort(Comparator.comparing(Share::fraction).reversed()
                .thenComparingInt(Share::index));
        for (int index = 0; index < remainder; index++) {
            int target = shares.get(index % shares.size()).index();
            floors.set(target, floors.get(target).add(BigInteger.ONE));
        }
        return floors.stream().map(value -> new BigDecimal(value).setScale(0)).toList();
    }

    private static List<BigDecimal> allocateEvenly(BigInteger total, int count) {
        BigInteger[] division = total.divideAndRemainder(BigInteger.valueOf(count));
        List<BigDecimal> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            BigInteger value = division[0].add(index < division[1].intValue() ? BigInteger.ONE : BigInteger.ZERO);
            result.add(new BigDecimal(value).setScale(0));
        }
        return result;
    }

    private static List<BigDecimal> zeros(int count) {
        return java.util.Collections.nCopies(count, BigDecimal.ZERO.setScale(0));
    }

    private static BigDecimal positive(BigDecimal value) {
        return value == null || value.signum() <= 0 ? BigDecimal.ZERO : value;
    }

    private record Share(int index, BigDecimal fraction) {
    }
}

package com.paper.mes.delivery.service;

import com.paper.mes.common.BusinessException;
import com.paper.mes.delivery.entity.DeliveryCustomerRevision;
import com.paper.mes.delivery.entity.DeliveryCustomerRevisionItem;
import com.paper.mes.delivery.entity.DeliveryDetail;
import com.paper.mes.delivery.entity.DeliveryOrder;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionItemMapper;
import com.paper.mes.delivery.mapper.DeliveryCustomerRevisionMapper;
import com.paper.mes.processorder.entity.FinishRoll;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * 在出库确认时冻结加工成品的客户展示口径。
 * 待出库阶段仍允许用户预览和发布人工更正版，确认后的历史单据不再回读可变的 FinishRoll。
 */
@Component
@RequiredArgsConstructor
public class DeliveryCustomerRevisionSnapshotWriter {

    private final DeliveryCustomerRevisionMapper revisionMapper;
    private final DeliveryCustomerRevisionItemMapper itemMapper;
    private final DeliveryCustomerRevisionReader revisionReader;

    public void freezeOnConfirm(DeliveryOrder order, List<DeliveryDetail> details,
                                Map<String, FinishRoll> finishes) {
        if (order == null || order.getUuid() == null || details == null || details.isEmpty()) {
            return;
        }
        List<String> detailUuids = details.stream().map(DeliveryDetail::getUuid).toList();
        Map<String, DeliveryCustomerRevisionItem> existing = revisionReader.latestItems(order.getUuid(), detailUuids);
        List<DeliveryDetail> missing = details.stream()
                .filter(detail -> !existing.containsKey(detail.getUuid()))
                .toList();
        if (missing.isEmpty()) {
            return;
        }

        int revisionNo = revisionReader.nextRevisionNo(order.getUuid());
        DeliveryCustomerRevision revision = new DeliveryCustomerRevision();
        revision.setDeliveryUuid(order.getUuid());
        revision.setRevisionNo(revisionNo);
        revision.setRequestId(DeliveryCustomerRevisionReader.SYSTEM_REQUEST_PREFIX + order.getUuid() + ":" + revisionNo);
        revision.setRequestHash(hash(revision.getRequestId()));
        revision.setReason(revisionNo == 1 ? "出库确认时系统冻结加工成品客户口径"
                : "出库再次确认时补冻结新增明细客户口径");
        revision.setItemCount(missing.size());

        BigDecimal total = BigDecimal.ZERO;
        revisionMapper.insert(revision);
        for (DeliveryDetail detail : missing) {
            FinishRoll finish = finishes.get(detail.getFinishUuid());
            Snapshot snapshot = snapshot(detail, finish);
            total = total.add(snapshot.customerWeight());
            itemMapper.insert(item(revision.getUuid(), detail, snapshot));
        }
        revision.setCustomerTotalWeight(total.setScale(3, RoundingMode.HALF_UP));
        revisionMapper.updateById(revision);
    }

    private Snapshot snapshot(DeliveryDetail detail, FinishRoll finish) {
        if (finish == null) {
            throw new BusinessException("出库确认关联的成品不存在：" + detail.getFinishRollNo());
        }
        String paperName = value(finish.getPaperName(), detail.getPaperName());
        Integer gramWeight = finish.getGramWeight();
        Integer finishWidth = finish.getFinishWidth();
        BigDecimal physicalWeight = positive(detail.getOutWeight(), "实物出库重量");
        if (paperName == null || gramWeight == null || gramWeight <= 0 || finishWidth == null || finishWidth <= 0) {
            throw new BusinessException("出库确认关联的成品实物规格不完整：" + detail.getFinishRollNo());
        }
        String customerPaperName = value(finish.getCustomerPaperName(), paperName);
        Integer customerGramWeight = finish.getCustomerGramWeight() == null
                ? gramWeight : finish.getCustomerGramWeight();
        Integer customerFinishWidth = finish.getCustomerFinishWidth() == null
                ? finishWidth : finish.getCustomerFinishWidth();
        BigDecimal customerWeight = resolveCustomerWeight(finish, physicalWeight);
        if (customerGramWeight <= 0 || customerFinishWidth <= 0) {
            throw new BusinessException("出库确认关联的客户口径不完整：" + detail.getFinishRollNo());
        }
        return new Snapshot(paperName, gramWeight, finishWidth, physicalWeight,
                customerPaperName, customerGramWeight, customerFinishWidth, customerWeight);
    }

    private DeliveryCustomerRevisionItem item(String revisionUuid, DeliveryDetail detail, Snapshot snapshot) {
        DeliveryCustomerRevisionItem item = new DeliveryCustomerRevisionItem();
        item.setRevisionUuid(revisionUuid);
        item.setDeliveryDetailUuid(detail.getUuid());
        item.setFinishUuid(detail.getFinishUuid());
        item.setPhysicalPaperName(snapshot.physicalPaperName());
        item.setPhysicalGramWeight(snapshot.physicalGramWeight());
        item.setPhysicalFinishWidth(snapshot.physicalFinishWidth());
        item.setPhysicalDeliveryWeight(snapshot.physicalWeight());
        item.setCustomerPaperName(snapshot.customerPaperName());
        item.setCustomerGramWeight(snapshot.customerGramWeight());
        item.setCustomerFinishWidth(snapshot.customerFinishWidth());
        item.setCustomerDisplayWeight(snapshot.customerWeight());
        item.setCalculationMode("KEEP");
        item.setRoundingScale(3);
        item.setRoundingMode("HALF_UP");
        item.setZeroPolicy("SKIP");
        return item;
    }

    private BigDecimal resolveCustomerWeight(FinishRoll finish, BigDecimal physicalWeight) {
        BigDecimal customerWeight = finish.getCustomerDisplayWeight();
        BigDecimal actualWeight = finish.getActualWeight();
        if (customerWeight == null || actualWeight == null || actualWeight.signum() <= 0) {
            return physicalWeight.setScale(3, RoundingMode.HALF_UP);
        }
        return customerWeight.multiply(physicalWeight)
                .divide(actualWeight, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal positive(BigDecimal value, String label) {
        if (value == null || value.signum() <= 0) throw new BusinessException(label + "必须大于0");
        return value;
    }

    private String value(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? fallback : preferred.trim();
    }

    private String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    private record Snapshot(String physicalPaperName, Integer physicalGramWeight,
                            Integer physicalFinishWidth, BigDecimal physicalWeight,
                            String customerPaperName, Integer customerGramWeight,
                            Integer customerFinishWidth, BigDecimal customerWeight) {
    }
}

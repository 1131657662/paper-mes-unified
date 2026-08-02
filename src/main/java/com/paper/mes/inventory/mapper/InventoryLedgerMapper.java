package com.paper.mes.inventory.mapper;

import com.paper.mes.inventory.entity.InventoryLedgerEntry;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** Restricted mapper: only append and read operations are exposed. */
public interface InventoryLedgerMapper {

    int insert(InventoryLedgerEntry entry);

    InventoryLedgerEntry selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    InventoryLedgerEntry selectLatestForUpdate(@Param("finishRollUuid") String finishRollUuid);

    List<InventoryLedgerEntry> selectByFinishUuid(@Param("finishRollUuid") String finishRollUuid);
}

package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PartialBackRecordInventoryContractTest {

    private static final Path AVAILABLE_FINISH = Path.of(
            "src/main/resources/mapper/delivery/AvailableFinishMapper.xml");
    private static final Path INVENTORY = Path.of(
            "src/main/resources/mapper/delivery/DeliveryInventoryMapper.xml");
    private static final Path WAREHOUSE_REPAIR = Path.of(
            "src/main/resources/mapper/delivery/DeliveryInventoryWarehouseRepairMapper.xml");
    private static final Path DELIVERY_DETAIL = Path.of(
            "src/main/resources/mapper/delivery/DeliveryDetailMapper.xml");

    @Test
    void deliveryQueriesIncludeRecordedInventoryFromPartiallyRecordedOrders() throws IOException {
        assertRecordedInventoryContract(read(AVAILABLE_FINISH));
        assertRecordedInventoryContract(read(INVENTORY));
        assertThat(read(WAREHOUSE_REPAIR)).contains("o.order_status IN (3, 4, 5)");
    }

    @Test
    void reopenBlocksActiveOrConfirmedDeliveryButIgnoresVoidedHistory() throws IOException {
        assertThat(read(DELIVERY_DETAIL)).contains(
                "dd.is_deleted = 0",
                "d.is_deleted = 0",
                "dd.stock_lock_status = 1 OR d.delivery_status = 2");
    }

    private void assertRecordedInventoryContract(String mapper) {
        assertThat(mapper)
                .contains("o.order_status IN (3, 4, 5)")
                .contains("f.finish_status = 2");
    }

    private String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}

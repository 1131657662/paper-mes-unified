package com.paper.mes.delivery;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryReceiverCustomerSchemaContractTest {

    @Test
    void migration_addsNullableReceiverCustomerWithoutBackfill() throws IOException {
        String migration = read("sql/V3.48__add_delivery_receiver_customer.sql");

        assertThat(migration).contains(
                "SET SESSION lock_wait_timeout = 5",
                "SELECT GET_LOCK('paper_mes_delivery_receiver_customer', 10)",
                "receiver_customer_name` VARCHAR(100) DEFAULT NULL",
                "收货客户名称（货主告知后手工填写）",
                "SELECT RELEASE_LOCK('paper_mes_delivery_receiver_customer')");
        assertThat(migration).doesNotContain("UPDATE biz_delivery_order");
    }

    @Test
    void baseline_containsNullableReceiverCustomer() throws IOException {
        assertThat(read("sql/01_schema_v4.1.sql"))
                .contains("`receiver_customer_name` VARCHAR(100) DEFAULT NULL");
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }
}

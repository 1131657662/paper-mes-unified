package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryCommandControllerContractTest {

    @Test
    void scrapControllerExposesOnlyThePermissionProtectedBusinessCommand() throws Exception {
        String source = read("src/main/java/com/paper/mes/inventory/controller/InventoryScrapController.java");

        assertThat(source).contains("@RequirePermission(Permissions.INVENTORY_SCRAP)")
                .contains("@PostMapping(\"/{finishUuid}/scrap\")")
                .contains("@Valid @RequestBody InventoryScrapDTO dto")
                .doesNotContain("@PutMapping")
                .doesNotContain("/status");
    }

    @Test
    void openingControllerUsesDataHealthPermissionAndExplicitOpeningRoute() throws Exception {
        String source = read("src/main/java/com/paper/mes/inventory/controller/InventoryOpeningController.java");

        assertThat(source).contains("@RequirePermission(Permissions.DATA_HEALTH)")
                .contains("@PostMapping(\"/opening/preview\")")
                .contains("@PostMapping(\"/opening\")")
                .contains("InventoryOpeningRequest");
    }

    private String read(String relativePath) throws Exception {
        return Files.readString(Path.of(relativePath), StandardCharsets.UTF_8);
    }
}

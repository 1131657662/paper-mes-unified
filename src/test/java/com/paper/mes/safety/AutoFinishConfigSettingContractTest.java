package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AutoFinishConfigSettingContractTest {

    @Test
    void autoFinishConfigSetting_defaultsToDisabled() throws IOException {
        String bootstrap = Files.readString(Path.of(
                "src/main/java/com/paper/mes/system/config/config/SystemConfigBootstrap.java"),
                StandardCharsets.UTF_8);
        String migration = Files.readString(Path.of(
                "sql/V3.43__add_auto_finish_config_setting.sql"),
                StandardCharsets.UTF_8);

        assertThat(bootstrap).contains(
                "process.autoFinishConfig", "成品配置允许自动生成", "\"false\", \"boolean\"");
        assertThat(migration).contains(
                "process.autoFinishConfig", "成品配置允许自动生成", "'false', 'boolean'");
    }
}

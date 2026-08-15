package com.paper.mes.ai.memory;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectMemoryVersionQueryServiceTest {

    @Test
    void versionsReturnMetadataWithoutDocumentBodies() {
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 15, 10, 30);
        when(repository.findVersions()).thenReturn(List.of(new ProjectMemoryVersionRow(
                "1.0.2", "1.0", "sha256:" + "0".repeat(64), "ACTIVE", "现场确认",
                "admin", "admin", createdAt)));

        var versions = new ProjectMemoryVersionQueryService(repository).versions();

        assertThat(versions).singleElement().satisfies(version -> {
            assertThat(version.memoryVersion()).isEqualTo("1.0.2");
            assertThat(version.status()).isEqualTo("ACTIVE");
            assertThat(version.createdAt()).isEqualTo(createdAt);
        });
    }
}

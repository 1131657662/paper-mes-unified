package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProjectMemorySeedImporterTest {

    @Test
    void importsOnlyWhenTheTableIsEmpty() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        ProjectMemoryDocumentProvider provider = mock(ProjectMemoryDocumentProvider.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(1);
        when(repository.hasAnyDocuments()).thenReturn(false);
        when(repository.insert(any(ProjectMemoryDocumentRow.class))).thenReturn(1);
        ProjectMemorySeedImporter importer = importer(jdbcTemplate, repository, provider);

        assertThat(importer.seedIfEmpty()).isTrue();
        verify(repository).insert(any(ProjectMemoryDocumentRow.class));
    }

    @Test
    void doesNotOverwriteAnExistingMemoryHistory() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        ProjectMemoryDocumentProvider provider = mock(ProjectMemoryDocumentProvider.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any(Object[].class)))
                .thenReturn(1);
        when(repository.hasAnyDocuments()).thenReturn(true);
        ProjectMemorySeedImporter importer = importer(jdbcTemplate, repository, provider);

        assertThat(importer.seedIfEmpty()).isFalse();
        verify(repository, never()).insert(any(ProjectMemoryDocumentRow.class));
    }

    private ProjectMemorySeedImporter importer(JdbcTemplate jdbcTemplate,
                                               ProjectMemoryDocumentRepository repository,
                                               ProjectMemoryDocumentProvider provider) {
        ObjectMapper objectMapper = new ObjectMapper();
        ProjectMemoryChecksum checksum = new ProjectMemoryChecksum(objectMapper);
        return new ProjectMemorySeedImporter(
                new AiProperties(), jdbcTemplate, new DefaultResourceLoader(), objectMapper,
                repository, new ProjectMemoryDocumentValidator(objectMapper, checksum), provider);
    }
}

package com.paper.mes.ai.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchOperation;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchRequest;
import com.paper.mes.common.BusinessException;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ProjectMemoryManagementServiceTest {

    @Test
    void patchCreatesNextSnapshotAndAudit() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode seed = mapper.readTree(Files.readString(Path.of("docs/ai/project-memory.seed.v1.json")));
        ProjectMemoryDocumentRow active = new ProjectMemoryDocumentRow("u1", "1.0.0", "1.0",
                seed.path("checksum").asText(), mapper.writeValueAsString(seed), "ACTIVE", "seed", "system", null);
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        ProjectMemoryDocumentProvider provider = mock(ProjectMemoryDocumentProvider.class);
        ProjectMemoryDocumentValidator validator = new ProjectMemoryDocumentValidator(mapper,
                new ProjectMemoryChecksum(mapper));
        when(repository.findAuditByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(repository.findActiveForUpdate()).thenReturn(Optional.of(active));
        when(repository.markSuperseded("1.0.0")).thenReturn(1);
        ProjectMemoryManagementService service = new ProjectMemoryManagementService(provider, repository, validator, mapper);

        var response = service.patch(new ProjectMemoryPatchRequest("1.0.0", List.of(
                new ProjectMemoryPatchOperation("replace", "/rules/rule-memory-safety/content",
                        mapper.readTree("\"updated\""))), "key-1", "现场确认"));

        assertThat(response.memoryVersion()).isEqualTo("1.0.1");
        verify(repository).markSuperseded("1.0.0");
        verify(repository).insert(any(ProjectMemoryDocumentRow.class));
        verify(repository).insertAudit(any(ProjectMemoryPatchAuditRow.class));
    }

    @Test
    void patchSkipsVersionAlreadyUsedByAFormerBranch() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode seed = mapper.readTree(Files.readString(Path.of("docs/ai/project-memory.seed.v1.json")));
        ProjectMemoryDocumentRow active = new ProjectMemoryDocumentRow("u1", "1.0.0", "1.0",
                seed.path("checksum").asText(), mapper.writeValueAsString(seed), "ACTIVE", "seed", "system", null);
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findAuditByIdempotencyKey("key-branch")).thenReturn(Optional.empty());
        when(repository.findActiveForUpdate()).thenReturn(Optional.of(active));
        when(repository.existsVersion("1.0.1")).thenReturn(true);
        when(repository.existsVersion("1.0.2")).thenReturn(false);
        when(repository.markSuperseded("1.0.0")).thenReturn(1);
        ProjectMemoryDocumentValidator validator = new ProjectMemoryDocumentValidator(mapper,
                new ProjectMemoryChecksum(mapper));
        ProjectMemoryManagementService service = new ProjectMemoryManagementService(mock(ProjectMemoryDocumentProvider.class),
                repository, validator, mapper);

        assertThat(service.patch(new ProjectMemoryPatchRequest("1.0.0", List.of(
                new ProjectMemoryPatchOperation("replace", "/rules/rule-memory-safety/content",
                        mapper.getNodeFactory().textNode("branch"))), "key-branch", "branch")).memoryVersion())
                .isEqualTo("1.0.2");
    }

    @Test
    void staleExpectedVersionReturnsConflictWithoutMutation() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findAuditByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(repository.findActiveForUpdate()).thenReturn(Optional.of(new ProjectMemoryDocumentRow(
                "u1", "1.0.3", "1.0", "sha256:" + "0".repeat(64), "{}", "ACTIVE", "", "system", null)));
        ProjectMemoryManagementService service = new ProjectMemoryManagementService(mock(ProjectMemoryDocumentProvider.class),
                repository, mock(ProjectMemoryDocumentValidator.class), mapper);

        assertThatThrownBy(() -> service.patch(new ProjectMemoryPatchRequest("1.0.2", List.of(
                new ProjectMemoryPatchOperation("replace", "/rules/r1/status", mapper.getNodeFactory().textNode("ACTIVE"))),
                "key-2", "stale"))).isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", 409);
        verify(repository, never()).insert(any(ProjectMemoryDocumentRow.class));
    }

    @Test
    void repeatedIdempotencyKeyReturnsOriginalSnapshot() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode seed = mapper.readTree(Files.readString(Path.of("docs/ai/project-memory.seed.v1.json")));
        var historicalDocument = (com.fasterxml.jackson.databind.node.ObjectNode) seed.deepCopy();
        historicalDocument.put("memoryVersion", "1.0.1");
        historicalDocument.put("checksum", new ProjectMemoryChecksum(mapper).calculate(historicalDocument));
        ProjectMemoryDocumentRow historical = new ProjectMemoryDocumentRow("u2", "1.0.1", "1.0",
                historicalDocument.path("checksum").asText(), mapper.writeValueAsString(historicalDocument),
                "SUPERSEDED", "patched", "admin", "admin");
        ProjectMemoryDocumentRepository repository = mock(ProjectMemoryDocumentRepository.class);
        when(repository.findAuditByIdempotencyKey("key-repeat")).thenReturn(Optional.of(
                new ProjectMemoryPatchAuditRow("a1", "key-repeat", "PATCH", "1.0.0", "1.0.0",
                        "1.0.1", seed.path("checksum").asText(), seed.path("checksum").asText(), "[]", "same", "admin")));
        when(repository.findVersionForUpdate("1.0.1")).thenReturn(Optional.of(historical));
        when(repository.findActiveForUpdate()).thenReturn(Optional.of(new ProjectMemoryDocumentRow(
                "u1", "1.0.0", "1.0", seed.path("checksum").asText(), mapper.writeValueAsString(seed),
                "ACTIVE", "seed", "system", null)));
        ProjectMemoryDocumentValidator validator = new ProjectMemoryDocumentValidator(mapper,
                new ProjectMemoryChecksum(mapper));
        ProjectMemoryManagementService service = new ProjectMemoryManagementService(mock(ProjectMemoryDocumentProvider.class),
                repository, validator, mapper);

        assertThat(service.patch(new ProjectMemoryPatchRequest("1.0.0", List.of(
                new ProjectMemoryPatchOperation("replace", "/rules/r1/status", mapper.getNodeFactory().textNode("ACTIVE"))),
                "key-repeat", "same")).memoryVersion()).isEqualTo("1.0.1");
        var order = inOrder(repository);
        order.verify(repository).findActiveForUpdate();
        order.verify(repository).findAuditByIdempotencyKey("key-repeat");
        verify(repository, never()).markSuperseded(anyString());
    }
}

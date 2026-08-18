package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.memory.ProjectMemoryManagementService;
import com.paper.mes.ai.memory.candidate.dto.ProjectMemoryCandidateApproveRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryPatchRequest;
import com.paper.mes.ai.memory.dto.ProjectMemoryResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectMemoryCandidateManagementServiceTest {

    @Test
    void approveAddsEditedExampleToExamplesRoot() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryManagementService memory = mock(ProjectMemoryManagementService.class);
        var row = new ProjectMemoryCandidateRow(
                "candidate-1", "example-candidate-1", "EXAMPLE",
                "{\"type\":\"EXAMPLE\",\"status\":\"ACTIVE\",\"scope\":\"SAW\",\"input\":\"旧内容\"}",
                "READY", 5, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), null, null, null);
        when(repository.findByUuidForUpdate("candidate-1")).thenReturn(Optional.of(row));
        when(repository.review(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(memory.patch(any())).thenReturn(new ProjectMemoryResponse(
                "1.0.2", "1.0", "sha256:" + "b".repeat(64), "READY",
                mapper.createObjectNode()));
        ProjectMemoryCandidateManagementService service =
                new ProjectMemoryCandidateManagementService(repository, memory, mapper);
        var edited = mapper.createObjectNode();
        edited.put("type", "EXAMPLE");
        edited.put("status", "ACTIVE");
        edited.put("scope", "SAW");
        edited.put("input", "新内容");

        service.approve("candidate-1", new ProjectMemoryCandidateApproveRequest(
                "1.0.1", "request-1", "现场确认", edited));

        ArgumentCaptor<ProjectMemoryPatchRequest> request =
                ArgumentCaptor.forClass(ProjectMemoryPatchRequest.class);
        verify(memory).patch(request.capture());
        assertThat(request.getValue().operations()).singleElement().satisfies(operation -> {
            assertThat(operation.path()).isEqualTo("/examples/example-candidate-1");
            assertThat(operation.value().path("input").asText()).isEqualTo("新内容");
        });
    }

    @Test
    void approveRejectsCandidateThatWasAlreadyActivated() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryManagementService memory = mock(ProjectMemoryManagementService.class);
        var row = new ProjectMemoryCandidateRow(
                "candidate-1", "example-candidate-1", "EXAMPLE",
                "{\"type\":\"EXAMPLE\",\"status\":\"ACTIVE\"}",
                "ACTIVE", 5, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), null, null, null);
        when(repository.findByUuidForUpdate("candidate-1")).thenReturn(Optional.of(row));
        ProjectMemoryCandidateManagementService service =
                new ProjectMemoryCandidateManagementService(repository, memory, mapper);

        assertThatThrownBy(() -> service.approve("candidate-1",
                new ProjectMemoryCandidateApproveRequest(
                        "1.0.1", "request-1", "repeat", null)))
                .hasMessageContaining("Memory candidate is not ready");
    }
}

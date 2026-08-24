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
import java.util.List;

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
        ProjectMemoryCandidateDocumentValidator validator = new ProjectMemoryCandidateDocumentValidator();
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
                new ProjectMemoryCandidateManagementService(repository, memory, mapper, validator);
        var edited = mapper.createObjectNode();
        edited.put("type", "EXAMPLE");
        edited.put("status", "ACTIVE");
        edited.put("scope", "SAW");
        edited.put("input", "新内容");
        edited.putObject("expected")
                .put("processType", "SAW")
                .put("intent", "CUTS")
                .put("field", "sawIntent/type");
        edited.put("evidenceRequired", true);
        edited.put("source", "confirmed-ai-candidate");

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
        ProjectMemoryCandidateDocumentValidator validator = new ProjectMemoryCandidateDocumentValidator();
        var row = new ProjectMemoryCandidateRow(
                "candidate-1", "example-candidate-1", "EXAMPLE",
                "{\"type\":\"EXAMPLE\",\"status\":\"ACTIVE\"}",
                "ACTIVE", 5, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), null, null, null);
        when(repository.findByUuidForUpdate("candidate-1")).thenReturn(Optional.of(row));
        ProjectMemoryCandidateManagementService service =
                new ProjectMemoryCandidateManagementService(repository, memory, mapper, validator);

        assertThatThrownBy(() -> service.approve("candidate-1",
                new ProjectMemoryCandidateApproveRequest(
                        "1.0.1", "request-1", "repeat", null)))
                .hasMessageContaining("Memory candidate is not ready");
    }

    @Test
    void approveRejectsLegacyRuleCandidateWithoutAControlledRuleSchema() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryManagementService memory = mock(ProjectMemoryManagementService.class);
        ProjectMemoryCandidateDocumentValidator validator = new ProjectMemoryCandidateDocumentValidator();
        var row = new ProjectMemoryCandidateRow(
                "candidate-rule", "rule-1", "RULE",
                "{\"type\":\"RULE\",\"status\":\"ACTIVE\"}",
                "READY", 5, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), null, null, null);
        when(repository.findByUuidForUpdate("candidate-rule")).thenReturn(Optional.of(row));

        ProjectMemoryCandidateManagementService service =
                new ProjectMemoryCandidateManagementService(repository, memory, mapper, validator);

        assertThatThrownBy(() -> service.approve("candidate-rule",
                new ProjectMemoryCandidateApproveRequest("1.0.1", "request-1", "review", null)))
                .hasMessageContaining("该类型不能进入正式项目记忆");
    }

    @Test
    void listRedactsLegacyCandidatePayloadToAReviewSummary() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryManagementService memory = mock(ProjectMemoryManagementService.class);
        ProjectMemoryCandidateDocumentValidator validator = new ProjectMemoryCandidateDocumentValidator();
        var row = new ProjectMemoryCandidateRow(
                "candidate-rule", "rule-1", "RULE",
                "{\"type\":\"RULE\",\"status\":\"READY\",\"content\":\"订单号:123456789 客户原话\"}",
                "READY", 1, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), null, null, null);
        when(repository.list(null)).thenReturn(List.of(row));
        ProjectMemoryCandidateManagementService service =
                new ProjectMemoryCandidateManagementService(repository, memory, mapper, validator);

        var response = service.list(null).getFirst();

        assertThat(response.candidate().size()).isEqualTo(3);
        assertThat(response.candidate().has("type")).isTrue();
        assertThat(response.candidate().has("status")).isTrue();
        assertThat(response.candidate().has("legacy")).isTrue();
        assertThat(response.candidate().toString()).doesNotContain("123456789", "客户原话", "content");
        assertThat(response.candidate().path("legacy").asBoolean()).isTrue();
    }

    @Test
    void detailRedactsLegacyCandidatePayloadAndEvidencePhrase() {
        ObjectMapper mapper = new ObjectMapper();
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        ProjectMemoryManagementService memory = mock(ProjectMemoryManagementService.class);
        ProjectMemoryCandidateDocumentValidator validator = new ProjectMemoryCandidateDocumentValidator();
        var row = new ProjectMemoryCandidateRow(
                "candidate-rule", "rule-1", "RULE",
                "{\"type\":\"RULE\",\"status\":\"READY\",\"content\":\"客户原话\"}",
                "READY", 1, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now().plusDays(30), null, null, null);
        when(repository.findByUuidForUpdate("candidate-rule")).thenReturn(Optional.of(row));
        when(repository.listEvidence("candidate-rule")).thenReturn(List.of(
                new ProjectMemoryCandidateEvidenceRow("evidence-1", "candidate-rule", "AI_CONFIRMED",
                        null, null, null, true, "admin", LocalDateTime.now())));
        ProjectMemoryCandidateManagementService service =
                new ProjectMemoryCandidateManagementService(repository, memory, mapper, validator);

        var response = service.detail("candidate-rule");

        assertThat(response.candidate().candidate().toString()).doesNotContain("客户原话", "content");
        assertThat(response.evidence()).singleElement().satisfies(evidence ->
                assertThat(evidence.phrase()).isNull());
    }
}

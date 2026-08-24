package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.process.session.crypto.AiMessageCipher;
import com.paper.mes.ai.process.session.crypto.AiMessageCryptoContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMemoryCandidateEvidenceAuditBackfillTest {

    @Test
    void processEncryptsLegacyTextBeforeClearingIt() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        LegacyEvidenceAuditContext context = new LegacyEvidenceAuditContext(
                "evidence-1", "客户原话", "{\"source\":\"legacy\"}",
                "{\"raw\":\"proposal\"}", "{\"raw\":\"final\"}",
                "{\"raw\":\"difference\"}");
        when(repository.findLegacyAuditContexts(100)).thenReturn(List.of(context));
        when(cipher.encrypt(any(AiMessageCryptoContext.class), anyString()))
                .thenReturn("v1:encrypted");
        when(cipher.hash(anyString())).thenReturn("audit-hash");
        when(repository.backfillAuditContext("evidence-1", "v1:encrypted", "audit-hash"))
                .thenReturn(1);
        ProjectMemoryCandidateEvidenceAuditBackfill service =
                new ProjectMemoryCandidateEvidenceAuditBackfill(repository, cipher, new ObjectMapper());

        service.process();

        ArgumentCaptor<String> plaintext = ArgumentCaptor.forClass(String.class);
        verify(cipher).encrypt(eq(new AiMessageCryptoContext(
                "evidence-1", 0, "MEMORY_EVIDENCE_AUDIT")), plaintext.capture());
        assertThat(plaintext.getValue()).doesNotContain("客户原话", "legacy", "proposal")
                .contains("phraseHash", "type", "text", "hash");
        verify(repository).backfillAuditContext("evidence-1", "v1:encrypted", "audit-hash");
    }

    @Test
    void malformedLegacyContextDoesNotPreventLaterRowsFromBeingProcessed() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        when(repository.findLegacyAuditContexts(100)).thenReturn(List.of(
                new LegacyEvidenceAuditContext("bad", "bad phrase", "{"),
                new LegacyEvidenceAuditContext("good", "safe phrase", "{\"ok\":true}")));
        when(cipher.encrypt(any(AiMessageCryptoContext.class), anyString()))
                .thenReturn("ciphertext");
        when(cipher.hash(anyString())).thenReturn("hash");
        when(repository.backfillAuditContext(anyString(), eq("ciphertext"), eq("hash")))
                .thenReturn(1);

        new ProjectMemoryCandidateEvidenceAuditBackfill(repository, cipher, new ObjectMapper()).process();

        verify(repository).backfillAuditContext("bad", "ciphertext", "hash");
        verify(repository).backfillAuditContext("good", "ciphertext", "hash");
    }

    @Test
    void processEncryptsLegacyRowsThatOnlyContainFinalEvidence() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        when(repository.findLegacyAuditContexts(100)).thenReturn(List.of(
                new LegacyEvidenceAuditContext("final-only", null, null, null,
                        "{\"processMode\":1}", null)));
        when(cipher.encrypt(any(AiMessageCryptoContext.class), anyString())).thenReturn("ciphertext");
        when(cipher.hash(anyString())).thenReturn("hash");
        when(repository.backfillAuditContext("final-only", "ciphertext", "hash")).thenReturn(1);

        new ProjectMemoryCandidateEvidenceAuditBackfill(repository, cipher, new ObjectMapper()).process();

        verify(repository).backfillAuditContext("final-only", "ciphertext", "hash");
    }

    @Test
    void processFinishesRowsThatWereAlreadyClearedWithoutAnAuditHash() {
        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        when(repository.findLegacyAuditContexts(100)).thenReturn(List.of(
                new LegacyEvidenceAuditContext("empty", null, null, null, null, null)));
        when(cipher.encrypt(any(AiMessageCryptoContext.class), anyString())).thenReturn("ciphertext");
        when(cipher.hash(anyString())).thenReturn("hash");
        when(repository.backfillAuditContext("empty", "ciphertext", "hash")).thenReturn(1);

        new ProjectMemoryCandidateEvidenceAuditBackfill(repository, cipher, new ObjectMapper()).process();

        verify(repository).backfillAuditContext("empty", "ciphertext", "hash");
    }

    @Test
    void processBoundsDeepAndWideLegacyShapes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ObjectNode current = root;
        for (int depth = 0; depth < 12; depth++) {
            current = current.putObject("level" + depth);
        }
        current.put("secret", "must-not-be-copied");
        ArrayNode wide = root.putArray("wide");
        for (int index = 0; index < 70; index++) {
            wide.add("value-" + index);
        }
        ObjectNode fields = root.putObject("fields");
        for (int index = 0; index < 70; index++) {
            fields.put("field-" + index, "value-" + index);
        }

        ProjectMemoryCandidateRepository repository = mock(ProjectMemoryCandidateRepository.class);
        AiMessageCipher cipher = mock(AiMessageCipher.class);
        LegacyEvidenceAuditContext context = new LegacyEvidenceAuditContext(
                "bounded", null, mapper.writeValueAsString(root));
        when(repository.findLegacyAuditContexts(100)).thenReturn(List.of(context));
        when(cipher.encrypt(any(AiMessageCryptoContext.class), anyString())).thenReturn("ciphertext");
        when(cipher.hash(anyString())).thenReturn("hash");
        when(repository.backfillAuditContext("bounded", "ciphertext", "hash")).thenReturn(1);

        new ProjectMemoryCandidateEvidenceAuditBackfill(repository, cipher, mapper).process();

        ArgumentCaptor<String> plaintext = ArgumentCaptor.forClass(String.class);
        verify(cipher).encrypt(any(AiMessageCryptoContext.class), plaintext.capture());
        JsonNode shaped = mapper.readTree(plaintext.getValue()).path("context");
        assertThat(plaintext.getValue()).doesNotContain("must-not-be-copied", "value-69");
        assertThat(shaped.path("fields").path("wide").path("truncated").asBoolean()).isTrue();
        assertThat(shaped.path("fields").path("wide").path("count").asInt()).isEqualTo(70);
        assertThat(shaped.path("fields").path("fields").path("truncated").asBoolean()).isTrue();
        assertThat(shaped.at("/fields/level0/fields/level1/fields/level2/fields/level3/fields/level4/fields/level5/fields/level6/fields/level7/type")
                .asText()).isEqualTo("truncated");
    }
}

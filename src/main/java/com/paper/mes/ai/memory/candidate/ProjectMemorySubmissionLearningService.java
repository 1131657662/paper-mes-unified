package com.paper.mes.ai.memory.candidate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.paper.mes.ai.process.security.ProcessTextRedactor;
import com.paper.mes.ai.process.session.ProcessAiConversationLearningReader;
import com.paper.mes.auth.context.AuthContextHolder;
import com.paper.mes.processorder.entity.OriginalRoll;
import com.paper.mes.processorder.entity.ProcessConfigDraft;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectMemorySubmissionLearningService {

    private static final Set<String> OMITTED_FIELDS = Set.of(
            "machineuuid", "originaluuid", "orderuuid", "unitprice", "billingamount");

    private final ProcessAiConversationLearningReader conversationReader;
    private final ProjectMemoryLearningOutboxService outboxService;
    private final ProcessTextRedactor redactor;
    private final ObjectMapper objectMapper;

    public Optional<ProjectMemorySubmissionLearningSnapshot> prepare(
            String orderUuid, List<OriginalRoll> rolls,
            Map<String, ProcessConfigDraft> drafts) {
        try {
            var conversation = conversationReader.read(orderUuid).orElse(null);
            if (conversation == null) return Optional.empty();
            ArrayNode sources = objectMapper.createArrayNode();
            ArrayNode configurations = objectMapper.createArrayNode();
            for (OriginalRoll roll : rolls) {
                ProcessConfigDraft draft = drafts.get(roll.getUuid());
                if (draft == null || !Integer.valueOf(1).equals(draft.getConfigStatus())) continue;
                ObjectNode source = source(roll);
                sources.add(source);
                ObjectNode item = configurations.addObject();
                item.set("source", source.deepCopy());
                item.put("processMode", draft.getProcessMode());
                item.put("mainStepType", draft.getMainStepType());
                item.set("plan", sanitizedJson(draft.getConfigJson()));
            }
            if (configurations.isEmpty()) return Optional.empty();
            String requirement = redactor.redact(
                    conversation.customerRequirement()).sanitizedText();
            ObjectNode context = objectMapper.createObjectNode();
            context.set("sourceRolls", sources);
            ObjectNode finalConfiguration = objectMapper.createObjectNode();
            finalConfiguration.set("processPlans", configurations);
            return Optional.of(new ProjectMemorySubmissionLearningSnapshot(
                    orderUuid, conversation.projectMemoryVersion(), requirement,
                    context, finalConfiguration, AuthContextHolder.currentDisplayName()));
        } catch (RuntimeException exception) {
            log.error("Could not prepare submitted-order memory evidence: orderUuid={}, type={}",
                    orderUuid, exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void captureInTransaction(ProjectMemorySubmissionLearningSnapshot snapshot) {
        outboxService.enqueueSubmitted(snapshot);
    }

    private ObjectNode source(OriginalRoll roll) {
        ObjectNode value = objectMapper.createObjectNode();
        value.put("ref", "R" + roll.getRowSort());
        put(value, "paperName", roll.getPaperName());
        put(value, "gramWeight", roll.getGramWeight());
        put(value, "widthMm", roll.getOriginalWidth());
        put(value, "storedDiameter", roll.getOriginalDiameter());
        put(value, "storedCoreDiameter", roll.getCoreDiameter());
        put(value, "pieceCount", roll.getPieceNum());
        return value;
    }

    private JsonNode sanitizedJson(String json) {
        try {
            JsonNode value = objectMapper.readTree(json);
            removeOmittedFields(value);
            return value;
        } catch (Exception exception) {
            throw new IllegalStateException("submitted process plan JSON is invalid", exception);
        }
    }

    private void removeOmittedFields(JsonNode node) {
        if (node == null) return;
        if (node.isArray()) {
            node.forEach(this::removeOmittedFields);
            return;
        }
        if (!node.isObject()) return;
        ObjectNode object = (ObjectNode) node;
        List<String> fields = new java.util.ArrayList<>();
        object.fieldNames().forEachRemaining(fields::add);
        for (String field : fields) {
            if (OMITTED_FIELDS.contains(field.toLowerCase(Locale.ROOT))) {
                object.remove(field);
            } else {
                removeOmittedFields(object.get(field));
            }
        }
    }

    private void put(ObjectNode target, String field, Object value) {
        if (value == null) return;
        target.set(field, objectMapper.valueToTree(value));
    }
}

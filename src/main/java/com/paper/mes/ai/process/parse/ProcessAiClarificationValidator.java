package com.paper.mes.ai.process.parse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paper.mes.ai.process.intent.ProcessAiClarificationQuestion;
import com.paper.mes.ai.process.security.ProcessAiIntentCipher;
import com.paper.mes.common.BusinessException;
import com.paper.mes.common.ResultCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/** Validates a clarification answer against the server-stored question envelope. */
@Service
public class ProcessAiClarificationValidator {

    private final ProcessAiParseRepository repository;
    private final ObjectMapper objectMapper;
    private final ProcessAiIntentCipher intentCipher;

    /** Compatibility constructor for legacy plaintext question fixtures. */
    ProcessAiClarificationValidator(ProcessAiParseRepository repository, ObjectMapper objectMapper) {
        this(repository, objectMapper, null);
    }

    @Autowired
    public ProcessAiClarificationValidator(ProcessAiParseRepository repository,
                                           ObjectMapper objectMapper,
                                           ProcessAiIntentCipher intentCipher) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.intentCipher = intentCipher;
    }

    public ProcessAiClarificationQuestion validate(String orderUuid, String conversationId,
                                                   int expectedVersion, String parseId,
                                                   Integer parseRevision, String questionId,
                                                   String answerCode, String answerText) {
        boolean structured = parseId != null || parseRevision != null || questionId != null
                || answerCode != null;
        if (!structured) {
            throw badRequest("AI_CLARIFICATION_CONTEXT_REQUIRED", "澄清回答必须绑定当前问题");
        }
        ProcessAiParseRecord record = repository.findLatestClarification(
                orderUuid, conversationId, expectedVersion).orElse(null);
        if (record == null) {
            throw conflict("AI_CLARIFICATION_NOT_ACTIVE", "当前没有待回答的AI澄清问题");
        }
        if (parseId == null || !parseId.equals(record.parseId())) {
            throw conflict("AI_CLARIFICATION_PARSE_CONFLICT", "澄清回答不属于当前AI解析");
        }
        if (parseRevision == null || parseRevision != record.parseRevision()) {
            throw conflict("AI_PARSE_REVISION_CONFLICT", "AI解析版本已过期，请重新获取问题");
        }
        ProcessAiClarificationQuestion question = findQuestion(record, questionId);
        if (question.parseRevision() != record.parseRevision()) {
            throw conflict("AI_PARSE_REVISION_CONFLICT", "澄清问题版本已过期，请重新获取问题");
        }
        boolean hasAnswerCode = answerCode != null && !answerCode.isBlank();
        if (hasAnswerCode && answerText != null && !answerText.isBlank()
                && !"ANSWER_TEXT".equals(answerCode)) {
            throw badRequest("AI_CLARIFICATION_TEXT_NOT_ALLOWED", "当前问题只能选择预设选项");
        }
        if (hasAnswerCode && "ANSWER_TEXT".equals(answerCode)
                && (answerText == null || answerText.isBlank() || isUnknown(answerText))) {
            throw badRequest("AI_CLARIFICATION_TEXT_REQUIRED", "请填写补充说明");
        }
        if (!hasAnswerCode) {
            if (isUnknown(answerText)) {
                if (!question.allowUnknown()) {
                    throw conflict("AI_CLARIFICATION_UNKNOWN_NOT_ALLOWED", "当前问题不支持不确定选项");
                }
                return question;
            }
            boolean textOption = question.options().stream()
                    .anyMatch(option -> "ANSWER_TEXT".equals(option.code()));
            if (!textOption) {
                throw badRequest("AI_CLARIFICATION_OPTION_REQUIRED", "请从当前澄清问题的选项中选择");
            }
            if (answerText == null || answerText.isBlank()) {
                throw badRequest("AI_CLARIFICATION_TEXT_REQUIRED", "请填写补充说明");
            }
            return question;
        }
        if (isUnknown(answerText) && !question.allowUnknown()) {
            throw conflict("AI_CLARIFICATION_UNKNOWN_NOT_ALLOWED", "当前问题不支持不确定选项");
        }
        if (question.options().stream().noneMatch(option -> option.code().equals(answerCode))
                && !isUnknown(answerCode)) {
            throw badRequest("AI_CLARIFICATION_OPTION_INVALID", "澄清选项无效");
        }
        if (isUnknown(answerCode) && !question.allowUnknown()) {
            throw conflict("AI_CLARIFICATION_UNKNOWN_NOT_ALLOWED", "当前问题不支持不确定选项");
        }
        return question;
    }

    public boolean isUnknown(String answerCode) {
        if (answerCode == null) return false;
        String normalized = answerCode.trim().toUpperCase(Locale.ROOT);
        return normalized.equals("UNKNOWN") || normalized.equals("UNSURE")
                || normalized.equals("NOT_SURE") || normalized.equals("不确定");
    }

    private ProcessAiClarificationQuestion findQuestion(ProcessAiParseRecord record,
                                                        String questionId) {
        String json = record.questionJson();
        if (intentCipher != null && json != null) {
            json = intentCipher.decrypt(record.conversationId(), record.parseRevision(), json);
        }
        if (questionId == null || json == null || json.isBlank()) {
            throw badRequest("AI_CLARIFICATION_CONTEXT_REQUIRED", "澄清回答缺少当前问题");
        }
        try {
            List<ProcessAiClarificationQuestion> questions = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(
                            List.class, ProcessAiClarificationQuestion.class));
            return questions.stream().filter(item -> questionId.equals(item.questionId()))
                    .findFirst().orElseThrow(() -> badRequest(
                            "AI_CLARIFICATION_QUESTION_INVALID", "澄清问题已失效"));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ResultCode.ERROR,
                    "AI_PARSE_STORED_DATA_INVALID", "Stored AI clarification data is invalid");
        }
    }

    private BusinessException badRequest(String code, String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, code, message);
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(ResultCode.CONFLICT, code, message);
    }
}

import type { ProcessAiClarificationOption, ProcessAiClarificationQuestion } from './types'

const UNKNOWN_OPTION: ProcessAiClarificationOption = { code: 'UNKNOWN', label: '不确定' }

export function displayedClarificationOptions(
  question: ProcessAiClarificationQuestion,
): ProcessAiClarificationOption[] {
  if (!question.allowUnknown || question.options.some((option) => option.code === UNKNOWN_OPTION.code)) {
    return question.options
  }
  return [...question.options, UNKNOWN_OPTION]
}

export function actionableClarificationOptions(
  question: ProcessAiClarificationQuestion,
): ProcessAiClarificationOption[] {
  return displayedClarificationOptions(question)
    .filter((option) => option.code !== 'ANSWER_TEXT')
}

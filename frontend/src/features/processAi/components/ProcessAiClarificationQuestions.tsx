import { Button } from 'antd'
import { actionableClarificationOptions, displayedClarificationOptions } from '../processAiClarificationModel'
import type { ProcessAiClarificationQuestion } from '../types'

interface Props {
  questions: ProcessAiClarificationQuestion[]
  onClarify: (question: ProcessAiClarificationQuestion, answerCode: string) => void
}

export default function ProcessAiClarificationQuestions({ questions, onClarify }: Props) {
  if (questions.length === 0) return null
  return <section className="process-ai-clarification-questions">
    {questions.map((question) => <Question key={question.questionId}
      question={question} onClarify={onClarify} />)}
  </section>
}

function Question({ question, onClarify }: {
  question: ProcessAiClarificationQuestion
  onClarify: Props['onClarify']
}) {
  const options = displayedClarificationOptions(question)
  const buttonOptions = actionableClarificationOptions(question)
  const needsText = options.some((option) => option.code === 'ANSWER_TEXT')
  return <div className="process-ai-clarification-question">
    <strong>{question.question}</strong>
    {buttonOptions.length > 0 && <div className="process-ai-clarification-question__options">
      {buttonOptions.map((option) => <Button key={option.code}
        onClick={() => onClarify(question, option.code)}>{option.label}</Button>)}
    </div>}
    {needsText && <small>请在下方输入补充说明后发送</small>}
  </div>
}

import { useEffect, useRef, useState, type Dispatch, type MutableRefObject,
  type SetStateAction } from 'react'
import { streamProcessAiParse, type ProcessAiStreamInput } from '../services/processAiStreamService'
import type { ProcessAiParseResult } from '../types'
import type { ProcessAiClarificationQuestion } from '../types'

interface Options {
  input: Omit<ProcessAiStreamInput, 'idempotencyKey' | 'action' | 'message'>
  onMessagesChanged: () => Promise<unknown>
}

export function useProcessAiStream(options: Options) {
  const controllerRef = useRef<AbortController | undefined>(undefined)
  const generationRef = useRef(0)
  const [state, setState] = useState<StreamState>(initialState)

  useEffect(() => () => {
    generationRef.current += 1
    controllerRef.current?.abort()
  }, [])

  const send = async (message: string) => {
    if (state.streaming) return
    const question = state.result?.clarificationQuestions?.[0]
    await execute({
      action: question ? 'CLARIFY' : 'START',
      idempotencyKey: crypto.randomUUID(),
      message,
      ...(question ? {
        parseId: state.result?.parseId,
        questionId: question.questionId,
        parseRevision: question.parseRevision,
        answerText: message,
      } : {}),
    })
  }

  const execute = async (attempt: StreamAttempt) => {
    const generation = generationRef.current + 1
    generationRef.current = generation
    controllerRef.current?.abort()
    const controller = new AbortController()
    controllerRef.current = controller
    setState({ streaming: true, pendingUser: attempt.message,
      progress: '正在读取母卷和项目规则…', retryAttempt: attempt })
    try {
      const updateState: Dispatch<SetStateAction<StreamState>> = (update) => {
        if (isCurrentRequest(generationRef, generation, controller)) setState(update)
      }
      const result = await runStream(options, attempt, controller, updateState)
      if (!isCurrentRequest(generationRef, generation, controller)) return
      // Message history is auxiliary. A failed refresh must not turn a
      // successful model response into an AI parse error.
      await options.onMessagesChanged().catch(() => undefined)
      if (!isCurrentRequest(generationRef, generation, controller)) return
      setState({ streaming: false, result })
    } catch (error) {
      if (!isCurrentRequest(generationRef, generation, controller)) return
      await options.onMessagesChanged().catch(() => undefined)
      if (!isCurrentRequest(generationRef, generation, controller)) return
      setState({ streaming: false, error: errorText(error),
        retryAttempt: isRetryable(error) ? attempt : undefined })
    }
  }

  const cancel = () => {
    generationRef.current += 1
    controllerRef.current?.abort()
    setState((current) => ({ streaming: false, error: '本次解析已停止',
      retryAttempt: current.retryAttempt }))
  }
  const clearResult = () => {
    generationRef.current += 1
    controllerRef.current?.abort()
    setState(initialState)
  }
  const replaceResult = (result: ProcessAiParseResult) => setState((current) => ({
    ...current, streaming: false, result, error: undefined, retryAttempt: undefined,
  }))
  const retry = async () => {
    if (state.retryAttempt && !state.streaming) await execute(state.retryAttempt)
  }
  const clarify = async (question: ProcessAiClarificationQuestion, answerCode: string) => {
    if (state.streaming || !state.result) return
    await execute({ action: 'CLARIFY', idempotencyKey: crypto.randomUUID(), message: answerCode,
      parseId: state.result.parseId, questionId: question.questionId, answerCode,
      parseRevision: question.parseRevision })
  }
  return { ...state, send, clarify, retry, cancel, clearResult, replaceResult }
}

function isCurrentRequest(
  generationRef: MutableRefObject<number>,
  generation: number,
  controller: AbortController,
) {
  return generationRef.current === generation && !controller.signal.aborted
}

async function runStream(
  options: Options,
  attempt: StreamAttempt,
  controller: AbortController,
  setState: Dispatch<SetStateAction<StreamState>>,
) {
  let result: ProcessAiParseResult | undefined
  await streamProcessAiParse({
    ...options.input,
    ...attempt,
  }, {
    onDelta: (count) => setState((current) => ({ ...current, progress: progressText(count) })),
    onResult: (value) => {
      result = value
      setState((current) => ({ ...current, result: value, progress: '正在生成可确认方案…' }))
    },
  }, controller.signal)
  if (!result) throw new Error('AI 未返回最终解析结果')
  return result
}

function progressText(count: number) {
  if (count < 240) return '正在识别客户原话中的工艺要求…'
  if (count < 900) return '正在匹配母卷和加工参数…'
  return '正在校验候选方案…'
}

function errorText(error: unknown) {
  return error instanceof Error ? error.message : 'AI 工艺解析失败'
}

function isRetryable(error: unknown) {
  return typeof error === 'object' && error !== null && 'retryable' in error
    ? error.retryable === true : true
}

interface StreamState {
  streaming: boolean
  pendingUser?: string
  progress?: string
  error?: string
  result?: ProcessAiParseResult
  retryAttempt?: StreamAttempt
}

interface StreamAttempt {
  action: 'START' | 'CLARIFY'
  idempotencyKey: string
  message: string
  parseId?: string
  questionId?: string
  answerCode?: string
  answerText?: string
  parseRevision?: number
}

const initialState: StreamState = { streaming: false }

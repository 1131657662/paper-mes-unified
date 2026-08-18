import { useEffect, useRef, useState } from 'react'
import { streamProcessAiParse, type ProcessAiStreamInput } from '../services/processAiStreamService'
import type { ProcessAiParseResult } from '../types'

interface Options {
  input: Omit<ProcessAiStreamInput, 'idempotencyKey' | 'action' | 'message'>
  hasHistory: boolean
  onMessagesChanged: () => Promise<unknown>
}

export function useProcessAiStream(options: Options) {
  const controllerRef = useRef<AbortController | undefined>(undefined)
  const [state, setState] = useState<StreamState>(initialState)

  useEffect(() => () => controllerRef.current?.abort(), [])

  const send = async (message: string) => {
    if (state.streaming) return
    await execute({
      action: options.hasHistory ? 'CLARIFY' : 'START',
      idempotencyKey: crypto.randomUUID(),
      message,
    })
  }

  const execute = async (attempt: StreamAttempt) => {
    const controller = new AbortController()
    controllerRef.current = controller
    setState({ streaming: true, pendingUser: attempt.message,
      progress: '正在读取母卷和项目规则…', retryAttempt: attempt })
    try {
      const result = await runStream(options, attempt, controller, setState)
      await options.onMessagesChanged()
      setState({ streaming: false, result })
    } catch (error) {
      if (controller.signal.aborted) return
      await options.onMessagesChanged().catch(() => undefined)
      setState({ streaming: false, error: errorText(error),
        retryAttempt: isRetryable(error) ? attempt : undefined })
    }
  }

  const cancel = () => {
    controllerRef.current?.abort()
    setState((current) => ({ streaming: false, error: '本次解析已停止',
      retryAttempt: current.retryAttempt }))
  }
  const clearResult = () => setState(initialState)
  const retry = async () => {
    if (state.retryAttempt && !state.streaming) await execute(state.retryAttempt)
  }
  return { ...state, send, retry, cancel, clearResult }
}

async function runStream(
  options: Options,
  attempt: StreamAttempt,
  controller: AbortController,
  setState: React.Dispatch<React.SetStateAction<StreamState>>,
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
}

const initialState: StreamState = { streaming: false }

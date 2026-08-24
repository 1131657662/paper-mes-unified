import type { ProcessAiParseResult } from '../types'

export interface ProcessAiStreamInput {
  orderUuid: string
  expectedVersion: number
  conversationId: string
  idempotencyKey: string
  action: 'START' | 'CLARIFY'
  message: string
  parseId?: string
  questionId?: string
  answerCode?: string
  answerText?: string
  parseRevision?: number
}

export interface ProcessAiStreamCallbacks {
  onDelta: (receivedCharacters: number) => void
  onResult: (result: ProcessAiParseResult) => void
}

export class ProcessAiStreamError extends Error {
  code: string
  retryable: boolean

  constructor(code: string, message: string, retryable: boolean) {
    super(message)
    this.name = 'ProcessAiStreamError'
    this.code = code
    this.retryable = retryable
  }
}

export async function streamProcessAiParse(
  input: ProcessAiStreamInput,
  callbacks: ProcessAiStreamCallbacks,
  signal: AbortSignal,
): Promise<void> {
  const response = await fetch(`/api/process-orders/${input.orderUuid}/ai/process-parse/stream`, {
    method: 'POST',
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      'X-Requested-With': 'XMLHttpRequest',
      'X-Request-Id': crypto.randomUUID(),
    },
    body: JSON.stringify({
      expectedVersion: input.expectedVersion,
      conversationId: input.conversationId,
      idempotencyKey: input.idempotencyKey,
      action: input.action,
      message: input.message,
      ...(input.parseId ? { parseId: input.parseId } : {}),
      ...(input.questionId ? { questionId: input.questionId } : {}),
      ...(input.answerCode ? { answerCode: input.answerCode } : {}),
      ...(input.answerText ? { answerText: input.answerText } : {}),
      ...(input.parseRevision ? { parseRevision: input.parseRevision } : {}),
    }),
    signal,
  })
  if (!response.ok) throw await responseError(response)
  if (!response.body) throw new ProcessAiStreamError('AI_STREAM_EMPTY', 'AI 流式响应不可用', true)
  await consumeSse(response.body, callbacks, signal)
}

async function consumeSse(
  stream: ReadableStream<Uint8Array>,
  callbacks: ProcessAiStreamCallbacks,
  signal: AbortSignal,
) {
  const reader = stream.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let receivedCharacters = 0
  try {
    while (!signal.aborted) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value, { stream: !done })
      const frames = splitFrames(buffer, done)
      buffer = frames.remainder
      for (const frame of frames.complete) {
        receivedCharacters += dispatchFrame(frame, callbacks, receivedCharacters)
      }
      if (done) return
    }
  } finally {
    await reader.cancel().catch(() => undefined)
  }
}

function splitFrames(buffer: string, flush: boolean) {
  const normalized = buffer.replaceAll('\r\n', '\n')
  const parts = normalized.split('\n\n')
  const remainder = flush ? '' : parts.pop() ?? ''
  return { complete: parts.filter(Boolean), remainder }
}

function dispatchFrame(
  frame: string,
  callbacks: ProcessAiStreamCallbacks,
  receivedCharacters: number,
): number {
  const event = frame.split('\n').find((line) => line.startsWith('event:'))?.slice(6).trim()
  const rawData = frame.split('\n').filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).trimStart()).join('\n')
  if (!event || !rawData) return 0
  const data: unknown = JSON.parse(rawData)
  if (event === 'delta') {
    const content = recordString(data, 'content') ?? ''
    callbacks.onDelta(receivedCharacters + content.length)
    return content.length
  }
  if (event === 'result') {
    callbacks.onResult(parseResult(data))
    return 0
  }
  if (event === 'error') throw streamEventError(data)
  return 0
}

function parseResult(value: unknown): ProcessAiParseResult {
  if (!isRecord(value) || typeof value.parseId !== 'string'
    || typeof value.conversationId !== 'string'
    || !isRecord(value.compiled)) {
    throw new ProcessAiStreamError('AI_STREAM_PROTOCOL', 'AI 返回结果格式无效', true)
  }
  if (isRecord(value.result)) return value as unknown as ProcessAiParseResult
  return { ...value, result: emptyExtraction(value.parseId) } as unknown as ProcessAiParseResult
}

function emptyExtraction(parseId: string): ProcessAiParseResult['result'] {
  return { parseId, schemaVersion: '2.0', assignments: [], unmappedText: [],
    conflicts: [], needsClarification: true, clarificationQuestions: [] }
}

function streamEventError(value: unknown) {
  return new ProcessAiStreamError(
    recordString(value, 'code') ?? 'AI_PROCESS_FAILED',
    recordString(value, 'message') ?? 'AI 工艺解析失败',
    isRecord(value) && value.retryable === true,
  )
}

async function responseError(response: Response) {
  const fallback = `AI 请求失败 (${response.status})`
  try {
    const body: unknown = await response.json()
    return new ProcessAiStreamError(
      recordString(body, 'errorCode') ?? 'AI_HTTP_ERROR',
      recordString(body, 'message') ?? fallback,
      response.status >= 500,
    )
  } catch {
    return new ProcessAiStreamError('AI_HTTP_ERROR', fallback, response.status >= 500)
  }
}

function recordString(value: unknown, key: string): string | undefined {
  return isRecord(value) && typeof value[key] === 'string' ? value[key] : undefined
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

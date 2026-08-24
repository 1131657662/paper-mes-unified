import type { ProcessAiMessage, ProcessAiParseResult } from './types'

export function latestStoredProcessAiResult(
  messages: ProcessAiMessage[], expectedVersion: number,
): ProcessAiParseResult | undefined {
  for (const message of messages.toReversed()) {
    if (message.role !== 'ASSISTANT') continue
    if (message.status !== 'FINAL') return undefined
    const result = parseStoredProcessAiResult(message.structuredResult)
    return result?.expectedVersion === expectedVersion ? result : undefined
  }
  return undefined
}

export function parseStoredProcessAiResult(value: string | undefined): ProcessAiParseResult | undefined {
  if (!value) return undefined
  try {
    const parsed: unknown = JSON.parse(value)
    if (!isRecord(parsed) || typeof parsed.parseId !== 'string'
      || typeof parsed.conversationId !== 'string' || !isRecord(parsed.compiled)) return undefined
    const result = parsed as unknown as ProcessAiParseResult
    return {
      ...result,
      result: isRecord(parsed.result) ? result.result : emptyExtraction(result.parseId),
      compiled: {
        ...result.compiled,
        packagingCandidates: Array.isArray(result.compiled.packagingCandidates)
          ? result.compiled.packagingCandidates
          : [],
      },
    }
  } catch {
    return undefined
  }
}

function emptyExtraction(parseId: string): ProcessAiParseResult['result'] {
  return { parseId, schemaVersion: '2.0', assignments: [], unmappedText: [],
    conflicts: [], needsClarification: true, clarificationQuestions: [] }
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

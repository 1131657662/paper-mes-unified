import { describe, expect, it } from 'vitest'
import { latestStoredProcessAiResult } from './processAiStoredResult'
import type { ProcessAiMessage, ProcessAiParseResult } from './types'

describe('latestStoredProcessAiResult', () => {
  it('does not restore an older result after the latest assistant attempt failed', () => {
    const previous = result('parse-1')
    const messages: ProcessAiMessage[] = [
      message(1, 'ASSISTANT', 'FINAL', JSON.stringify(previous)),
      message(2, 'USER', 'FINAL'),
      message(3, 'ASSISTANT', 'FAILED'),
    ]

    expect(latestStoredProcessAiResult(messages, 1)).toBeUndefined()
  })

  it('restores the latest final assistant result', () => {
    const expected = result('parse-2')
    const messages: ProcessAiMessage[] = [
      message(1, 'ASSISTANT', 'FINAL', JSON.stringify(result('parse-1'))),
      message(2, 'ASSISTANT', 'FINAL', JSON.stringify(expected)),
    ]

    expect(latestStoredProcessAiResult(messages, 1)?.parseId).toBe('parse-2')
  })
})

function message(sequenceNo: number, role: ProcessAiMessage['role'],
  status: ProcessAiMessage['status'], structuredResult?: string): ProcessAiMessage {
  return { sequenceNo, role, status, content: '', structuredResult, createdAt: '2026-08-17T10:00:00' }
}

function result(parseId: string): ProcessAiParseResult {
  return {
    conversationId: 'conversation-1', parseId, parseRevision: 1, expectedVersion: 1,
    status: 'READY', expiresAt: '2026-08-17T10:30:00',
    baseline: { plans: [] },
    result: { parseId, schemaVersion: '1.0', assignments: [], unmappedText: [], conflicts: [],
      needsClarification: false, clarificationQuestions: [] },
    compiled: { eligible: true, plans: [], packagingCandidates: [], errors: [], warnings: [] },
  }
}

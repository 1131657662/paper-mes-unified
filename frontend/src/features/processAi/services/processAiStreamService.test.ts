import { afterEach, describe, expect, it, vi } from 'vitest'
import { streamProcessAiParse } from './processAiStreamService'

afterEach(() => vi.unstubAllGlobals())

describe('streamProcessAiParse', () => {
  it('consumes structured deltas as progress and exposes only the final result', async () => {
    const fetchMock = vi.fn().mockResolvedValue(sseResponse([
      'event: conversation\ndata: {"conversationId":"conversation-1"}',
      'event: delta\ndata: {"content":"{\\"parse"}',
      `event: result\ndata: ${JSON.stringify(resultData())}`,
      'event: done\ndata: {"done":true}',
    ]))
    vi.stubGlobal('fetch', fetchMock)
    const deltas: number[] = []
    const results: string[] = []

    await streamProcessAiParse(streamInput(), {
      onDelta: (count) => deltas.push(count),
      onResult: (result) => results.push(result.parseId),
    }, new AbortController().signal)

    expect(deltas).toEqual([7])
    expect(results).toEqual(['parse-1'])
    expect(fetchMock).toHaveBeenCalledWith(expect.stringContaining('/stream'),
      expect.objectContaining({ method: 'POST' }))
  })
})

function sseResponse(frames: string[]) {
  return new Response(`${frames.join('\n\n')}\n\n`, {
    status: 200,
    headers: { 'Content-Type': 'text/event-stream' },
  })
}

function streamInput() {
  return {
    orderUuid: 'order-1', expectedVersion: 3, conversationId: 'conversation-1',
    idempotencyKey: 'request-1', action: 'START' as const, message: '切2刀',
  }
}

function resultData() {
  return {
    conversationId: 'conversation-1', parseId: 'parse-1', parseRevision: 1,
    expectedVersion: 3, status: 'READY', expiresAt: '2026-08-16T12:00:00Z',
    result: { parseId: 'parse-1', schemaVersion: '1.0', assignments: [], unmappedText: [],
      conflicts: [], needsClarification: false, clarificationQuestions: [] },
    compiled: { eligible: true, plans: [], packagingCandidates: [], errors: [], warnings: [] },
  }
}

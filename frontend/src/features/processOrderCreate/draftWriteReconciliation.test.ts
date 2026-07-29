import { describe, expect, it, vi } from 'vitest'
import { BizError } from '../../api/request'
import type { DraftOrderVO } from '../../types/processOrder'
import {
  batchPlanMatches,
  progressMatches,
  rollProcessesMatch,
  runReconciledDraftWrite,
  singlePlanMatches,
} from './draftWriteReconciliation'

vi.mock('antd', () => ({
  message: { error: vi.fn(), success: vi.fn() },
}))

const draft = (version: number): DraftOrderVO => ({
  currentStep: 3,
  order: { uuid: 'order-1', version } as DraftOrderVO['order'],
  rolls: [{
    uuid: 'roll-1', processMode: 1, mainStepType: 2, machineUuid: 'machine-1',
  }],
  configs: [{
    originalUuid: 'roll-1', configType: 'singlePlan',
    plan: { processMode: 1, mainStepType: 2, unitPrice: 321 },
    preview: { originalUuid: 'roll-1', ready: true },
  }],
})

describe('draft write reconciliation', () => {
  it('recovers a committed write after an uncertain response', async () => {
    const result = await runReconciledDraftWrite({
      expectedVersion: 7,
      isApplied: (latest) => progressMatches(latest, 3),
      readLatest: vi.fn().mockResolvedValue(draft(8)),
      recoverData: () => undefined,
      write: vi.fn().mockRejectedValue(new BizError('Bad Gateway', 502)),
    })

    expect(result).toEqual({ data: undefined, recovered: true, version: 8 })
  })

  it('does not hide an uncertain response when the target state was not committed', async () => {
    vi.useFakeTimers()
    try {
      const error = new BizError('Bad Gateway', 502)
      const resultPromise = runReconciledDraftWrite({
        expectedVersion: 8,
        isApplied: (latest) => progressMatches(latest, 4),
        readLatest: vi.fn().mockResolvedValue(draft(8)),
        recoverData: () => undefined,
        write: vi.fn().mockRejectedValue(error),
      })
      const rejection = expect(resultPromise).rejects.toBe(error)

      await vi.runAllTimersAsync()

      await rejection
    } finally {
      vi.useRealTimers()
    }
  })

  it('recovers when a later refresh observes the committed draft', async () => {
    vi.useFakeTimers()
    try {
      const readLatest = vi.fn()
        .mockResolvedValueOnce(draft(7))
        .mockResolvedValueOnce(draft(8))
      const resultPromise = runReconciledDraftWrite({
        expectedVersion: 7,
        isApplied: (latest) => progressMatches(latest, 3),
        readLatest,
        recoverData: () => 'recovered',
        write: vi.fn().mockRejectedValue(new BizError('timeout', 502)),
      })

      await vi.advanceTimersByTimeAsync(500)

      await expect(resultPromise).resolves.toEqual({
        data: 'recovered', recovered: true, version: 8,
      })
      expect(readLatest).toHaveBeenCalledTimes(2)
    } finally {
      vi.useRealTimers()
    }
  })

  it('matches process settings and plan targets rather than version alone', () => {
    const latest = draft(8)
    expect(rollProcessesMatch(latest, {
      expectedVersion: 7,
      rolls: [{
        originalUuid: 'roll-1', processMode: 1, mainStepType: 2, machineUuid: 'machine-1',
      }],
    })).toBe(true)
    expect(singlePlanMatches(latest, {
      expectedVersion: 7,
      originalUuid: 'roll-1',
      plan: { processMode: 1, mainStepType: 2, unitPrice: 321 },
    })).toBe(true)
    expect(batchPlanMatches(latest, {
      expectedVersion: 7,
      originalUuids: ['roll-1'],
      plan: { processMode: 1, mainStepType: 2, unitPrice: 999 },
    })).toBe(false)
  })
})

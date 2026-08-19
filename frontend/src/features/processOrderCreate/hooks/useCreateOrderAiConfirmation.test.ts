import { describe, expect, it } from 'vitest'
import type { ProcessAiPackagingDraft } from '../../processAi/types'
import {
  consumeProcessAiPackagingDraft,
  mergeProcessAiPackagingDrafts,
  pendingPackagingInput,
} from './useCreateOrderAiConfirmation'

describe('create order AI packaging drafts', () => {
  it('stores confirmed packaging candidates by original roll', () => {
    const drafts = mergeProcessAiPackagingDrafts({}, [packagingDraft('roll-1', 'parse-1')])

    expect(drafts['roll-1']).toMatchObject({
      parseId: 'parse-1',
      values: { originalUuid: 'roll-1', stepType: 4 },
    })
  })

  it('replaces an older candidate for the same original roll', () => {
    const current = { 'roll-1': packagingDraft('roll-1', 'parse-1') }

    const drafts = mergeProcessAiPackagingDrafts(current, [packagingDraft('roll-1', 'parse-2')])

    expect(drafts['roll-1']?.parseId).toBe('parse-2')
  })

  it('removes only the draft consumed by manual save or cancel', () => {
    const current = {
      'roll-1': packagingDraft('roll-1', 'parse-1'),
      'roll-2': packagingDraft('roll-2', 'parse-1'),
    }

    const drafts = consumeProcessAiPackagingDraft(current, 'roll-1')

    expect(drafts).toEqual({ 'roll-2': current['roll-2'] })
  })

  it('does not query packaging candidates before the process-mode step', () => {
    expect(pendingPackagingInput({ current: 1, orderUuid: 'order-1', draftVersion: 2 })).toBeUndefined()
  })

  it('queries packaging candidates from the process-mode step with the current version', () => {
    expect(pendingPackagingInput({ current: 2, orderUuid: 'order-1', draftVersion: 3 }))
      .toEqual({ orderUuid: 'order-1', expectedVersion: 3 })
  })
})

function packagingDraft(originalUuid: string, parseId: string): ProcessAiPackagingDraft {
  return {
    parseId,
    ownerRollRef: originalUuid,
    values: {
      originalUuid,
      stepType: 4,
      stepName: '包膜',
      isMain: 0,
      billingMode: 2,
      billingBasis: 'PIECE',
      serviceQuantity: 2,
      unitPrice: 20,
      remark: 'AI 识别结果，保存前人工确认',
    },
  }
}

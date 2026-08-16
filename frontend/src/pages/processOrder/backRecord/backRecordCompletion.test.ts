import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  backRecordSelectionError,
  buildBackRecordCompleteDTO,
  isCompletionOnly,
} from './backRecordCompletion'

describe('backRecordCompletion', () => {
  it('recognizes completion when no source roll remains', () => {
    expect(
      isCompletionOnly({
        completeOrder: true,
        remainingCount: 0,
        selectedCount: 0,
      }),
    ).toBe(true)
  })

  it('builds a command without warehouse or historical details', () => {
    const detail = {
      order: { uuid: 'order-1', version: 29 },
    } as ProcessOrderDetailVO

    const result = buildBackRecordCompleteDTO(detail)

    expect(result).toEqual({
      expectedVersion: 29,
      releaseAdminPassword: undefined,
      releaseAdminUsername: undefined,
      releaseReason: undefined,
      varianceReason: undefined,
    })
    expect(result).not.toHaveProperty('rolls')
    expect(result).not.toHaveProperty('warehouseUuid')
  })

  it('rejects partial selection when completing the order', () => {
    expect(
      backRecordSelectionError({
        completeOrder: true,
        remainingCount: 2,
        selectedCount: 1,
      }),
    ).toContain('全部未回录')
  })
})

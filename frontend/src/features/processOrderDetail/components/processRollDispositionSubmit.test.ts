import { afterEach, describe, expect, it, vi } from 'vitest'
import type { OriginalRoll } from '../../../types/processOrder'
import {
  buildDispositionDto,
  disposeSelectedRolls,
  eligibleRolls,
  normalizeRollSelection,
  successText,
  type ProcessRollDispositionFormValues,
} from './processRollDispositionSubmit'

describe('未加工母卷处置提交规则', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('只返回仍可处置的母卷', () => {
    const rolls = eligibleRolls([
      roll({ uuid: 'pending', rollStatus: 2 }),
      roll({ uuid: 'checked', isChecked: 1 }),
      roll({ uuid: 'done', rollStatus: 3 }),
      roll({ uuid: 'disposed', dispositionAction: 'CANCEL' }),
    ])

    expect(rolls.map((item) => item.uuid)).toEqual(['pending'])
  })

  it('为直发母卷保留仓库、称重和当前单据版本', () => {
    vi.stubGlobal('crypto', { randomUUID: () => 'request-1' })
    const values: ProcessRollDispositionFormValues = {
      action: 'DIRECT_SHIP',
      reason: '客户改为直发',
      warehouseUuid: 'warehouse-1',
      actualWeight: 512.5,
    }

    expect(buildDispositionDto(values, 8)).toEqual({
      action: 'DIRECT_SHIP',
      requestId: 'request-1',
      reason: '客户改为直发',
      expectedOrderVersion: 8,
      warehouseUuid: 'warehouse-1',
      actualWeight: 512.5,
    })
  })

  it('批量取消时明确显示已处理卷数', () => {
    expect(successText('CANCEL', 3)).toContain('3 卷')
    expect(successText('CANCEL', 1)).not.toContain('1 卷')
  })

  it('切换到单卷动作时只保留第一卷', () => {
    expect(normalizeRollSelection('DIRECT_SHIP', ['roll-1', 'roll-2'])).toEqual(['roll-1'])
    expect(normalizeRollSelection('SPLIT_TO_ORDER', ['roll-1', 'roll-2'])).toEqual(['roll-1'])
    expect(normalizeRollSelection('CANCEL', ['roll-1', 'roll-2'])).toEqual(['roll-1', 'roll-2'])
  })

  it('网络重试复用同一业务请求号', async () => {
    const dispose = vi.fn().mockResolvedValue({})
    const requestIds = new Map<string, string>()
    const requestIdFor = (key: string) => {
      const existing = requestIds.get(key)
      if (existing) return existing
      requestIds.set(key, 'request-stable')
      return 'request-stable'
    }
    const params = {
      rollUuids: ['roll-1'],
      orderUuid: 'order-1',
      expectedOrderVersion: 8,
      values: { action: 'CANCEL' as const, reason: '客户取消加工' },
      dispose,
      requestIdFor,
      onApplied: vi.fn(),
    }

    await disposeSelectedRolls(params)
    await disposeSelectedRolls(params)

    expect(dispose.mock.calls[0]?.[0].dto.requestId).toBe('request-stable')
    expect(dispose.mock.calls[1]?.[0].dto.requestId).toBe('request-stable')
  })
})

function roll(overrides: Partial<OriginalRoll> = {}): OriginalRoll {
  return { uuid: 'roll-1', rollStatus: 2, isChecked: 0, ...overrides }
}

import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBackRecordMetrics } from './backRecordMetrics'
import { buildBackRecordDTO, type BackRecordFormValues } from './backRecordUtils'
import { nextAddedFinishIndex, visibleFinishEntries } from './backRecordFinishAdjustment'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'

describe('标准加工实际产出调整', () => {
  it('4件计划实际产出2件时不再要求未产出卷填写重量', () => {
    const detail = standardDetail()
    const values = filledValues(['finish-1', 'finish-2'])
    values.finishAdjustments = {
      'roll-roll-1': {
        plannedFinishUuids: detail.finishRolls.map((finish) => finish.uuid),
        producedFinishUuids: ['finish-1', 'finish-2'],
        reason: '现场只完成2卷',
        added: [],
      },
    }

    const metrics = buildBackRecordMetrics(detail, values)
    const dto = buildBackRecordDTO(detail, values)

    expect(metrics.missingOfficialFinishWeight).toBe(0)
    expect(metrics.finishCount).toBe(2)
    expect(dto.finishes?.filter((finish) => finish.productionAction === 'NOT_PRODUCED').map((finish) => finish.uuid))
      .toEqual(['finish-3', 'finish-4'])
  })

  it('4件计划实际产出6件时新增成品进入回录DTO和统计', () => {
    const detail = standardDetail()
    const added = {
      uuid: 'added-roll-roll-1-0',
      originalUuid: 'roll-1',
      finishWidth: 600,
      actualWeight: 40,
    }
    const values = filledValues(detail.finishRolls.map((finish) => finish.uuid))
    values.finishAdjustments = {
      'roll-roll-1': {
        plannedFinishUuids: detail.finishRolls.map((finish) => finish.uuid),
        producedFinishUuids: detail.finishRolls.map((finish) => finish.uuid),
        reason: '现场实际多产出2卷',
        added: [added, { ...added, uuid: 'added-roll-roll-1-1', actualWeight: 42 }],
      },
    }
    values.finishes = {
      ...values.finishes,
      [added.uuid]: { originalUuid: 'roll-1', finishWidth: 600, actualWeight: 40 },
      'added-roll-roll-1-1': { originalUuid: 'roll-1', finishWidth: 600, actualWeight: 42 },
    }

    const metrics = buildBackRecordMetrics(detail, values)
    const dto = buildBackRecordDTO(detail, values)

    expect(metrics.finishCount).toBe(6)
    expect(metrics.missingOfficialFinishWeight).toBe(0)
    expect(dto.finishes?.filter((finish) => finish.productionAction === 'ADDED')).toHaveLength(2)
    expect(dto.finishes?.filter((finish) => finish.productionAction === 'ADDED').map((finish) => finish.originalUuid))
      .toEqual(['roll-1', 'roll-1'])
  })
  it('deleting a middle added item does not reuse its temporary key', () => {
    const added = [
      { uuid: 'added-roll-roll-1-0', originalUuid: 'roll-1' },
      { uuid: 'added-roll-roll-1-2', originalUuid: 'roll-1' },
    ]

    expect(nextAddedFinishIndex('roll-roll-1', added)).toBe(3)
  })

  it('hides planned finishes marked as not produced from the entry list', () => {
    const detail = standardDetail()
    const item = buildBackRecordWorkbench(detail).items[0]
    expect(item).toBeDefined()
    if (!item) return

    const visible = visibleFinishEntries(item, {
      plannedFinishUuids: detail.finishRolls.map((finish) => finish.uuid),
      producedFinishUuids: ['finish-1', 'finish-2'],
      reason: 'actual output was lower than planned',
      added: [],
    })

    expect(visible.map(({ finish }) => finish.uuid)).toEqual(['finish-1', 'finish-2'])
  })
})

function standardDetail(): ProcessOrderDetailVO {
  const finishRolls = ['finish-1', 'finish-2', 'finish-3', 'finish-4'].map((uuid, index) => ({
    uuid,
    finishRollNo: `F00000${index + 1}`,
    finishWidth: 600,
    estimateWeight: 90,
    isSpare: 0,
    isRemain: 0,
    rollNoStatus: 1,
    sourceType: 1,
  }))
  return {
    order: { uuid: 'order-1', version: 1 },
    originalRolls: [{ uuid: 'roll-1', processMode: 1, originalWidth: 2400 }],
    rolls: [],
    finishRolls,
    steps: [],
    rollProductions: [{
      originalUuid: 'roll-1',
      processMode: 1,
      finishes: finishRolls.map((finish) => ({ uuid: finish.uuid })),
    }],
  }
}

function filledValues(produced: string[]): BackRecordFormValues {
  return {
    warehouseUuid: 'warehouse-1',
    rolls: { 'roll-1': { actualWeight: 400 } },
    finishes: Object.fromEntries(produced.map((uuid) => [uuid, { actualWeight: 90 }])),
    steps: {},
  }
}

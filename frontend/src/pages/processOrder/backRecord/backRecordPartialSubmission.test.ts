import { describe, expect, it } from 'vitest'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBackRecordDTO, type BackRecordFormValues } from './backRecordUtils'
import {
  buildBackRecordValidationPaths,
  selectedFinishUuidsForSubmission,
} from './backRecordValidationPaths'

describe('部分回录提交范围', () => {
  it('只提交当前选中母卷及其成品', () => {
    const dto = buildBackRecordDTO(twoRollDetail(), filledValues(), undefined, undefined, {
      completeOrder: false,
      selectedFinishUuids: new Set(['finish-1']),
      selectedRollUuids: new Set(['roll-1']),
    })

    expect(dto.rolls.map((roll) => roll.uuid)).toEqual(['roll-1'])
    expect(dto.finishes?.map((finish) => finish.uuid)).toEqual(['finish-1'])
    expect(dto.steps?.map((step) => step.uuid)).toEqual(['step-1'])
  })

  it('只为选中批次生成表单校验路径', () => {
    const paths = buildBackRecordValidationPaths({
      completeOrder: false,
      detail: twoRollDetail(),
      selectedFinishUuids: new Set(['finish-1']),
      selectedItemKeys: new Set(['roll-roll-1']),
      selectedRollUuids: new Set(['roll-1']),
    })

    expect(paths).toContainEqual(['rolls', 'roll-1'])
    expect(paths).toContainEqual(['finishes', 'finish-1'])
    expect(paths).toContainEqual(['steps', 'step-1'])
    expect(paths).not.toContainEqual(['rolls', 'roll-2'])
    expect(paths).not.toContainEqual(['finishes', 'finish-2'])
  })

  it('将用户明确来源的历史成品归入对应批次', () => {
    const detail = twoRollDetail()
    detail.rollProductions = []
    const values = filledValues()
    values.finishes = { 'finish-1': { actualWeight: 90, originalUuid: 'roll-1' } }

    const selected = selectedFinishUuidsForSubmission(
      detail,
      values,
      new Set(),
      new Set(['roll-1']),
    )

    expect(selected).toEqual(new Set(['finish-1']))
  })
})

function twoRollDetail(): ProcessOrderDetailVO {
  return {
    order: { uuid: 'order-1', version: 3 },
    originalRolls: [
      { uuid: 'roll-1', processMode: 1 },
      { uuid: 'roll-2', processMode: 1 },
    ],
    rolls: [],
    finishRolls: [
      { uuid: 'finish-1', isSpare: 0, rollNoStatus: 1, sourceType: 1 },
      { uuid: 'finish-2', isSpare: 0, rollNoStatus: 1, sourceType: 1 },
    ],
    steps: [
      { uuid: 'step-1', originalUuid: 'roll-1' },
      { uuid: 'step-2', originalUuid: 'roll-2' },
    ],
    rollProductions: [
      { originalUuid: 'roll-1', finishes: [{ uuid: 'finish-1' }], steps: [{ uuid: 'step-1', originalUuid: 'roll-1' }] },
      { originalUuid: 'roll-2', finishes: [{ uuid: 'finish-2' }], steps: [{ uuid: 'step-2', originalUuid: 'roll-2' }] },
    ],
  }
}

function filledValues(): BackRecordFormValues {
  return {
    warehouseUuid: 'warehouse-1',
    rolls: { 'roll-1': { actualWeight: 100 }, 'roll-2': { actualWeight: 100 } },
    finishes: { 'finish-1': { actualWeight: 90 }, 'finish-2': { actualWeight: 90 } },
    steps: { 'step-1': { lossWeight: 10 }, 'step-2': { lossWeight: 10 } },
  }
}

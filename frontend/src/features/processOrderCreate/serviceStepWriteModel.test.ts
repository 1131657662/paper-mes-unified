import { describe, expect, it } from 'vitest'
import type { ProcessStep } from '../../types/processOrder'
import {
  resolveServiceStepWriteTarget,
  serviceStepIsAbsent,
  serviceStepMatchesRequest,
} from './serviceStepWriteModel'

describe('附加工艺写入目标', () => {
  it('版本同步后发现同类工艺时改为更新', () => {
    const steps = [step('step-1', 3)]

    const target = resolveServiceStepWriteTarget(steps, 3)

    expect(target).toEqual({ kind: 'update', stepUuid: 'step-1' })
  })

  it('没有同类工艺时创建新记录', () => {
    expect(resolveServiceStepWriteTarget([], 4)).toEqual({ kind: 'create' })
  })

  it('发现重复同类工艺时阻止继续写入', () => {
    const steps = [step('step-1', 3), step('step-2', 3)]

    expect(resolveServiceStepWriteTarget(steps, 3)).toEqual({ kind: 'duplicate' })
  })

  it('刷新结果已不存在目标时跳过重复删除', () => {
    expect(serviceStepIsAbsent([], 'deleted-step')).toBe(true)
    expect(serviceStepIsAbsent(undefined, 'deleted-step')).toBe(false)
  })

  it('matches persisted pricing fields for uncertain-write recovery', () => {
    expect(serviceStepMatchesRequest({
      uuid: 'step-1', originalUuid: 'roll-1', stepType: 3, isMain: 0,
      billingMode: 1, billingBasis: 'PIECE', unitPrice: 2,
    }, {
      originalUuid: 'roll-1', stepType: 3, isMain: 0,
      billingMode: 1, billingBasis: 'piece', unitPrice: 2,
    })).toBe(true)
  })
})

function step(uuid: string, stepType: number): ProcessStep {
  return { uuid, stepType, originalUuid: 'roll-1', isMain: 0 }
}

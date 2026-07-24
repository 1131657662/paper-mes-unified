import { describe, expect, it } from 'vitest'
import type { Machine, MachineCapability } from '../../types/machine'
import { applyDefaultMachineToRoll, machinesForStep, suggestedMachineUuid } from './machineDefaults'
import type { RollDraft } from './types'

describe('加工资源默认选择', () => {
  it('多台兼容资源时自动选择当前工艺默认资源', () => {
    const machines = [machine('normal'), machine('preferred', { defaultCapability: true })]

    const result = applyDefaultMachineToRoll(roll(), machines)

    expect(result.machineUuid).toBe('preferred')
  })

  it('多台兼容资源且没有默认时保留空值等待人工选择', () => {
    const machines = [machine('one'), machine('two')]

    expect(applyDefaultMachineToRoll(roll(), machines).machineUuid).toBeUndefined()
  })

  it('已有兼容选择优先于默认资源', () => {
    const machines = [machine('selected'), machine('preferred', { defaultCapability: true })]

    const result = applyDefaultMachineToRoll(roll({ machineUuid: 'selected' }), machines)

    expect(result.machineUuid).toBe('selected')
  })

  it('超出门幅和卷重范围的资源不进入候选', () => {
    const machines = [machine('limited', { maxWidth: 1000, maxRollWeight: 500 })]

    const result = machinesForStep(1, machines, { width: 1200, weight: 800 })

    expect(result).toEqual([])
  })

  it('多件同规格母卷按单卷重量校验机台能力', () => {
    const machines = [machine('limited', { maxRollWeight: 1000 })]

    const result = applyDefaultMachineToRoll(roll({ rollWeight: 800, pieceNum: 2 }), machines)

    expect(result.machineUuid).toBe('limited')
  })

  it('没有能力数据时兼容历史通用机台', () => {
    const legacy: Machine = { uuid: 'legacy', machineName: '通用机', machineType: 3, status: 1 }

    expect(machinesForStep(2, [legacy]).map((item) => item.uuid)).toEqual(['legacy'])
  })

  it('服务工序切换后用默认工位替换不兼容的主工艺机台', () => {
    const machines = [
      machine('rewind', { stepType: 2, processName: '复卷' }),
      machine('strip-default', {
        stepType: 3,
        processName: '剥损整理',
        defaultCapability: true,
      }),
    ]

    expect(suggestedMachineUuid({
      mainStepType: 3,
      currentMachineUuid: 'rewind',
      machines,
      context: { width: 1000, weight: 250 },
    })).toBe('strip-default')
  })
})

function machine(uuid: string, capabilityPatch: Partial<MachineCapability> = {}): Machine {
  return {
    uuid, machineName: uuid, status: 1,
    capabilities: [{
      catalogUuid: 'saw', stepType: 1, processCode: 'SAW', processName: '锯纸',
      processCategory: 'PRODUCTION', defaultCapability: false, priority: 100,
      ...capabilityPatch,
    }],
  }
}

function roll(patch: Partial<RollDraft> = {}): RollDraft {
  return {
    localId: 'roll', paperName: '白卡', gramWeight: 300, originalWidth: 1200,
    rollWeight: 800, mainStepType: 1, processMode: 1, ...patch,
  }
}

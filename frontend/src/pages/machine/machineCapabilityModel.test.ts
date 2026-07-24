import { describe, expect, it } from 'vitest'
import type { MachineCapability } from '../../types/machine'
import { capabilitiesToForm, capabilityRangeText, clearCapabilityDefaults } from './machineCapabilityModel'

describe('机台能力档案模型', () => {
  it('详情能力转换为可保存表单值', () => {
    const values = capabilitiesToForm([capability({ defaultCapability: true })])

    expect(values[0]).toMatchObject({ catalogUuid: 'saw', isDefault: 1, priority: 20 })
  })

  it('停用资源时清除全部默认标记', () => {
    const values = clearCapabilityDefaults([{ catalogUuid: 'saw', isDefault: 1 }])

    expect(values[0]?.isDefault).toBe(0)
  })

  it('加工范围组合为紧凑说明', () => {
    const text = capabilityRangeText(capability({
      minWidth: 500, maxWidth: 2400, maxRollWeight: 3000, maxDiameter: 1500,
    }))

    expect(text).toBe('门幅 500-2400 mm · 卷重 <= 3000 kg · 卷径 <= 1500 mm')
  })
})

function capability(patch: Partial<MachineCapability> = {}): MachineCapability {
  return {
    catalogUuid: 'saw', stepType: 1, processCode: 'SAW', processName: '锯纸',
    processCategory: 'PRODUCTION', defaultCapability: false, priority: 20, ...patch,
  }
}

import { describe, expect, it } from 'vitest'
import type { OriginalRoll } from '../../../types/processOrder'
import { DEFAULT_DISPOSITION_VALUES, buildRollOptions, filterRollOption } from './processRollDispositionOptions'

describe('未加工母卷处置选项', () => {
  it('用序号、身份和规格区分同品名母卷', () => {
    const options = buildRollOptions([
      roll({
        batchNo: 'B-01',
        actualGramWeight: 266,
        actualWidth: 1680,
        originalDiameter: 1300,
        coreDiameter: 3,
        rollWeight: 500,
        pieceNum: 2,
      }),
      roll({ uuid: 'roll-2', rowSort: 2, rollNo: 'R-002' }),
    ])

    expect(options[0]?.label).toBe('母卷 1 · 未记录卷号 · 测试')
    expect(options[1]?.label).toContain('母卷 2 · 卷号 R-002 · 测试')
    expect(options[0]?.detail).toContain('批次 B-01')
    expect(options[0]?.detail).toContain('克重 266 g')
    expect(options[0]?.detail).toContain('门幅 1680 mm')
    expect(options[0]?.detail).toContain('卷径 1300 mm')
    expect(options[0]?.detail).toContain('纸芯 3" (76 mm)')
    expect(options[0]?.detail).toContain('总重 1000 kg')
    expect(options[0]?.fields).toEqual([
      { label: '卷号', value: '未记录' },
      { label: '编号', value: '-' },
      { label: '批次', value: 'B-01' },
      { label: '克重', value: '266 g' },
      { label: '门幅', value: '1680 mm' },
      { label: '卷径', value: '1300 mm' },
      { label: '纸芯', value: '3" (76 mm)' },
      { label: '总重', value: '1000 kg' },
    ])
  })

  it('按卷号、批次和规格搜索母卷', () => {
    const option = buildRollOptions([roll({ batchNo: 'B-01', originalWidth: 1702 })])[0]!

    expect(filterRollOption('b-01', option)).toBe(true)
    expect(filterRollOption('1702', option)).toBe(true)
    expect(filterRollOption('roll-1', option)).toBe(true)
    expect(filterRollOption('not-found', option)).toBe(false)
  })

  it('打开弹窗时不预选任何母卷', () => {
    expect(DEFAULT_DISPOSITION_VALUES).toEqual({ action: 'CANCEL' })
    expect('rollUuid' in DEFAULT_DISPOSITION_VALUES).toBe(false)
  })
})

function roll(overrides: Partial<OriginalRoll> = {}): OriginalRoll {
  return { uuid: 'roll-1', rowSort: 1, paperName: '测试', ...overrides }
}

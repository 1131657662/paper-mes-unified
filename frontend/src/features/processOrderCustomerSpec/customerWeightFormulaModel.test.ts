import { describe, expect, it } from 'vitest'
import { STANDARD_WEIGHT_FORMULA } from './customerSpecDraftModel'
import {
  appendFormulaToken,
  formulaPresetValue,
  toDisplayFormula,
  toEngineFormula,
} from './customerWeightFormulaModel'

describe('客户重量公式编辑模型', () => {
  it('标准公式显示为中文业务表达式', () => {
    expect(toDisplayFormula(STANDARD_WEIGHT_FORMULA)).toBe(
      '实物重量 × (客户克重 ÷ 实物克重) × (客户门幅 ÷ 实物门幅)',
    )
  })

  it('中文公式转换为后端可执行表达式', () => {
    const formula = toEngineFormula('实物重量 ×（客户克重 ÷ 实物克重）')

    expect(formula).toBe('physicalWeight * (customerGsm / physicalGsm)')
  })

  it('自定义公式追加中文变量后仍保存后端变量', () => {
    const formula = appendFormulaToken('physicalWeight +', '客户门幅')

    expect(formula).toBe('physicalWeight + customerWidth')
    expect(formulaPresetValue(formula)).toBe('CUSTOM')
  })

  it('历史公式中的扩展变量也显示为中文', () => {
    expect(toDisplayFormula('finishWeight - lossWeight + adjustment')).toBe(
      '成品重量 - 损耗重量 + 调整值',
    )
  })
})

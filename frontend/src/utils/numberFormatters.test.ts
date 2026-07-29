import { describe, expect, it } from 'vitest'
import {
  formatFixedNumberInput,
  formatFractionAsPercent,
  formatStoredDiameter,
  formatStoredCoreDiameter,
  formatWholeKg,
} from './numberFormatters'

describe('重量小数格式化', () => {
  it('客户重量输入框按整公斤显示', () => {
    expect(formatFixedNumberInput(1171.1)).toBe('1171')
  })

  it('客户重量汇总按整公斤四舍五入', () => {
    expect(formatWholeKg(9361.5)).toBe('9362 kg')
  })
})

describe('工艺数值格式化', () => {
  it('把后端归一化比例显示为百分比', () => {
    expect(formatFractionAsPercent(1)).toBe('100%')
    expect(formatFractionAsPercent(0.255)).toBe('25.5%')
  })

  it('兼容历史英寸和现行毫米卷径', () => {
    expect(formatStoredDiameter(3)).toBe('3" (76 mm)')
    expect(formatStoredDiameter(1200)).toBe('1200 mm')
    expect(formatStoredCoreDiameter(76)).toBe('76 mm')
    expect(formatStoredCoreDiameter(3)).toBe('3" (76 mm)')
  })
})

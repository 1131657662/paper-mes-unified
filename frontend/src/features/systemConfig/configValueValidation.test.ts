import { describe, expect, it } from 'vitest'
import { CONFIG_KEYS } from './configFallbacks'
import { getConfigValueError } from './configValueValidation'

describe('系统参数业务范围校验', () => {
  it.each(['1.5', '-1', '101'])('拒绝非法备用卷号数量 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.spareRollNoCount, value)).toBe('备用卷号数量必须是 0 到 100 的整数')
  })

  it.each(['0', '8', '100', '1.0'])('接受合法备用卷号数量 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.spareRollNoCount, value)).toBeUndefined()
  })

  it.each(['9', '101', '20.5'])('拒绝非法默认分页条数 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.defaultPageSize, value)).toBe('默认每页条数必须是 10 到 100 的整数')
  })

  it.each(['6', '3651', '7.5'])('拒绝非法备份保留天数 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.backupRetentionDays, value)).toBe('备份保留天数必须是 7 到 3650 天的整数')
  })

  it.each(['-1', '100.01'])('拒绝越界百分比 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.weightTolerancePercent, value)).toBe('百分比参数必须在 0 到 100 之间')
  })

  it.each(['-0.01', '1.001', '1000000000'])('拒绝非法金额 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.discountMaxAmount, value)).toBe(
      '金额参数必须在 0 到 999999999.99 之间，且最多保留两位小数',
    )
  })

  it.each(['-0.01', '1000000000'])('拒绝非法计价免审上限 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.pricingAutoApproveLimit, value)).toBe(
      '金额参数必须在 0 到 999999999.99 之间，且最多保留两位小数',
    )
  })

  it.each(['-1', '3', '1.5'])('拒绝非法现结拦截模式 %s', (value) => {
    expect(errorFor(CONFIG_KEYS.cashSettleBlockMode, value)).toBe('现结出库拦截模式只能是 0、1 或 2')
  })
})

function errorFor(configKey: string, value: string) {
  return getConfigValueError({ configKey, value, valueType: 'number' })
}

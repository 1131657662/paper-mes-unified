import { describe, expect, it } from 'vitest'
import { CONFIG_KEYS } from './configFallbacks'
import { getConfigValueError } from './configValueValidation'

describe('备用卷号数量配置校验', () => {
  it.each(['1.5', '-1', '101'])('拒绝非法值 %s', (value) => {
    expect(getConfigValueError({
      configKey: CONFIG_KEYS.spareRollNoCount,
      value,
      valueType: 'number',
    })).toBe('备用卷号数量必须是 0 到 100 的整数')
  })

  it.each(['0', '8', '100', '1.0'])('接受合法整数值 %s', (value) => {
    expect(getConfigValueError({
      configKey: CONFIG_KEYS.spareRollNoCount,
      value,
      valueType: 'number',
    })).toBeUndefined()
  })
})

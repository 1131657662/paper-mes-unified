import { describe, expect, it } from 'vitest'
import { operatorOptions, optionLabel, processOptions, scopeOptions, signalOptions } from './reportAlertRuleLabels'

describe('报表阈值规则标签', () => {
  it('显示各类规则配置的业务名称', () => {
    expect(optionLabel(signalOptions, 'LOSS_RATIO')).toBe('损耗率')
    expect(optionLabel(scopeOptions, 2)).toBe('指定客户')
    expect(optionLabel(operatorOptions, 'GTE')).toBe('大于等于')
    expect(optionLabel(processOptions, 2)).toBe('复卷')
  })

  it('未知配置显示占位符', () => {
    expect(optionLabel(signalOptions, 'UNKNOWN')).toBe('-')
  })
})

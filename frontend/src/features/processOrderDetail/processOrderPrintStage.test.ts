import { describe, expect, it } from 'vitest'
import { hasConfirmedProcessOrderPrint, hasHistoricalUnconfirmedPrint,
  resolveProcessOrderStatus } from './processOrderPrintStage'

describe('resolveProcessOrderStatus', () => {
  it('maps a server stage to the corresponding execution status', () => {
    expect(resolveProcessOrderStatus('PENDING_MANUAL_CONFIRM', 1)).toBe(2)
  })

  it('falls back to the legacy numeric status for old responses', () => {
    expect(resolveProcessOrderStatus(undefined, 3)).toBe(3)
  })

  it('uses draft as the default when neither field is available', () => {
    expect(resolveProcessOrderStatus('UNKNOWN')).toBe(0)
  })
})

describe('历史打印确认风险', () => {
  it('打印状态和次数同时有效才视为人工确认', () => {
    expect(hasConfirmedProcessOrderPrint(1, 1)).toBe(true)
    expect(hasConfirmedProcessOrderPrint(1, 0)).toBe(false)
    expect(hasConfirmedProcessOrderPrint(0, 1)).toBe(false)
  })

  it.each([4, 5])('状态为 %s 且未确认打印时标记历史风险', (status) => {
    expect(hasHistoricalUnconfirmedPrint(status, false)).toBe(true)
    expect(hasHistoricalUnconfirmedPrint(status, true)).toBe(false)
  })
})

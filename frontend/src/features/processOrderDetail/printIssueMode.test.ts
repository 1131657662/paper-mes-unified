import { describe, expect, it } from 'vitest'
import { resolvePrintIssueMode } from './printIssueMode'

describe('resolvePrintIssueMode', () => {
  it('待下发且未打印时进入首次下发模式', () => {
    expect(resolvePrintIssueMode(1, 0)).toBe('issue')
  })

  it('加工中且已打印时进入补打模式', () => {
    expect(resolvePrintIssueMode(2, 1)).toBe('reprint')
  })

  it('已下发但未确认打印时进入未打印模式', () => {
    expect(resolvePrintIssueMode(2, 0, 'ISSUED', 0)).toBe('unprinted')
  })

  it.each([3, 4, 5])('状态为 %s 时进入审计补打模式', (status) => {
    expect(resolvePrintIssueMode(status, 1)).toBe('audited-reprint')
  })

  it('异常状态组合不会误触发下发或补打', () => {
    expect(resolvePrintIssueMode(1, 1)).toBe('preview')
    expect(resolvePrintIssueMode(2, 0)).toBe('preview')
  })

  it('待回录及已完成版本进入审计补打模式', () => {
    expect(resolvePrintIssueMode(3, 1, 'ISSUED', 1)).toBe('audited-reprint')
    expect(resolvePrintIssueMode(4, 1, 'FINISHED', 1)).toBe('audited-reprint')
  })

  it('完工版本始终只读打印', () => {
    expect(resolvePrintIssueMode(2, 1, 'FINISHED')).toBe('preview')
  })
})

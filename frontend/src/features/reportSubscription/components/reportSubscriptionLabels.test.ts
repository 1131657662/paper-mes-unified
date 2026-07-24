import { describe, expect, it } from 'vitest'
import type { ReportSubscription } from '../types'
import { scheduleLabel } from './reportSubscriptionLabels'

describe('报表订阅计划文案', () => {
  it('每周计划同时显示星期与时间', () => {
    expect(scheduleLabel(subscription({ scheduleType: 2, weekDay: 1 }))).toBe('每周一 08:30')
  })

  it('每月计划显示执行日', () => {
    expect(scheduleLabel(subscription({ scheduleType: 3, monthDay: 15 }))).toBe('每月 15 日 08:30')
  })
})

function subscription(overrides: Partial<ReportSubscription>): ReportSubscription {
  return {
    uuid: 'subscription-1', subscriptionName: '日报', reportPath: '/reports/overview', scheduleType: 1,
    executionTime: '08:30:00', timezone: 'Asia/Shanghai', reportQuery: {},
    periodPolicy: 1, releasePolicy: 1, isEnabled: 1, nextRunAt: '2026-07-21T08:30:00',
    version: 1, recipients: [], ...overrides,
  }
}

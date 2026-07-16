import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import type { DashboardTrend } from '../../../types/dashboard'

export interface DashboardTrendModel {
  averageAmount: number
  hasReceivable: boolean
  maxAmount: number
  monthly: DashboardTrend[]
  totalAmount: number
  totalOrders: number
}

export function buildTrendModel(monthly: DashboardTrend[], now: Dayjs = dayjs()): DashboardTrendModel {
  const displayMonthly = fillRecentMonths(monthly, now)
  const maxAmount = Math.max(...displayMonthly.map((item) => Number(item.amount ?? 0)), 0)
  const totalAmount = displayMonthly.reduce((sum, item) => sum + Number(item.amount ?? 0), 0)
  const totalOrders = displayMonthly.reduce((sum, item) => sum + Number(item.orderCount ?? 0), 0)
  return {
    averageAmount: totalAmount / 12,
    hasReceivable: maxAmount > 0,
    maxAmount,
    monthly: displayMonthly,
    totalAmount,
    totalOrders,
  }
}

function fillRecentMonths(monthly: DashboardTrend[], now: Dayjs): DashboardTrend[] {
  const byMonth = new Map(monthly.map((item) => [item.month, item]))
  return Array.from({ length: 12 }, (_, index) => {
    const month = now.subtract(11 - index, 'month').format('YYYY-MM')
    return byMonth.get(month) ?? { month, amount: 0, finishWeight: 0, orderCount: 0, originalWeight: 0 }
  })
}

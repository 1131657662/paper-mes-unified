import type { ReportPeriodPolicy, ReportScheduleType, ReportSubscription } from '../types'

const weekDays = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']

export function scheduleLabel(subscription: ReportSubscription): string {
  const time = subscription.executionTime.slice(0, 5)
  if (subscription.scheduleType === 1) return `每天 ${time}`
  if (subscription.scheduleType === 2) return `每${weekDays[subscription.weekDay ?? 1]} ${time}`
  return `每月 ${subscription.monthDay} 日 ${time}`
}

export const scheduleOptions = [
  { label: '每天', value: 1 },
  { label: '每周', value: 2 },
  { label: '每月', value: 3 },
] satisfies Array<{ label: string; value: ReportScheduleType }>

export const periodOptions = [
  { label: '上一日', value: 1 },
  { label: '上一周', value: 2 },
  { label: '上一月', value: 3 },
  { label: '固定区间', value: 4 },
] satisfies Array<{ label: string; value: ReportPeriodPolicy }>

export const weekDayOptions = weekDays.slice(1).map((label, index) => ({ label, value: index + 1 }))

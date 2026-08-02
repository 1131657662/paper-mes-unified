import dayjs, { type Dayjs } from 'dayjs'
import type { SettleOrder } from '../../../types/settle'

export type CollectionTone = 'danger' | 'warning' | 'success' | 'secondary'

export interface SettleCollectionDisplay {
  text: string
  detail: string
  tone: CollectionTone
  active: boolean
  overdue: boolean
}

type CollectionFields = Pick<SettleOrder, 'settleStatus' | 'unreceivedAmount' | 'dueDate'>

export function resolveSettleCollectionDisplay(
  order: CollectionFields,
  today: Dayjs = dayjs(),
): SettleCollectionDisplay {
  if (order.settleStatus === 3) return settledDisplay()
  if (order.settleStatus === 4) return voidDisplay()
  if (Number(order.unreceivedAmount ?? 0) <= 0) return settledDisplay()
  if (!order.dueDate) return {
    text: '未设置到期日', detail: '可提醒，暂不计算逾期', tone: 'secondary', active: true, overdue: false,
  }
  return dueDateDisplay(order.dueDate, today)
}

function dueDateDisplay(value: string, today: Dayjs): SettleCollectionDisplay {
  const days = dayjs(value).startOf('day').diff(today.startOf('day'), 'day')
  if (days < 0) return {
    text: `${value} · 逾期 ${Math.abs(days)} 天`, detail: '需要跟进', tone: 'danger', active: true, overdue: true,
  }
  if (days === 0) return {
    text: `${value} · 今日到期`, detail: '需要跟进', tone: 'warning', active: true, overdue: false,
  }
  return {
    text: `${value} · ${days} 天后`, detail: '需要跟进', tone: 'warning', active: true, overdue: false,
  }
}

function settledDisplay(): SettleCollectionDisplay {
  return { text: '已结清', detail: '无需催收', tone: 'success', active: false, overdue: false }
}

function voidDisplay(): SettleCollectionDisplay {
  return { text: '已作废', detail: '无需催收', tone: 'secondary', active: false, overdue: false }
}

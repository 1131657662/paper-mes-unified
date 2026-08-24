import { Tag } from 'antd'

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: '有效',
  PARTIAL_ROLLED_BACK: '部分回滚',
  FULL_ROLLED_BACK: '全部回滚',
  PRICE_PENDING: '待定价',
  CONFIRMED: '已定价',
  VOIDED: '已作废',
  PENDING: '待处理',
  APPLIED: '已处理',
  REVERSED: '已反向',
  CANCELLED: '已取消',
  REQUESTED: '待审批',
  APPROVED: '已审批',
  PAID: '已支付',
  IN_OWN_STOCK: '我方库存',
  EMPTY: '已清空',
}

export function statusLabel(value?: string) {
  return value ? (STATUS_LABELS[value] ?? value) : '-'
}

export function StatusTag({ value }: { value?: string }) {
  const color = value === 'PAID' || value === 'CONFIRMED' || value === 'IN_OWN_STOCK'
    ? 'green'
    : value === 'PRICE_PENDING' || value === 'PENDING' || value === 'REQUESTED'
      ? 'gold'
      : value === 'CANCELLED' || value === 'VOIDED' || value === 'FULL_ROLLED_BACK'
        ? 'default'
        : 'blue'
  return <Tag color={color}>{statusLabel(value)}</Tag>
}

export function formatWeight(value?: number) {
  return value == null ? '-' : `${value.toFixed(3)} kg`
}

export function formatAmount(value?: number) {
  return value == null ? '-' : `${Math.round(value).toLocaleString('zh-CN')} 元`
}

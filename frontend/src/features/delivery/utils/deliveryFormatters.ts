import type { AvailableFinishVO } from '../../../types/delivery'

export function formatKg(value?: number) {
  return `${(value ?? 0).toLocaleString('zh-CN', {
    maximumFractionDigits: 3,
    minimumFractionDigits: 3,
  })}kg`
}

export function formatTon(value?: number) {
  return `${((value ?? 0) / 1000).toLocaleString('zh-CN', {
    maximumFractionDigits: 3,
    minimumFractionDigits: 3,
  })}t`
}

export function finishSpecText(item: AvailableFinishVO) {
  const parts = [
    item.gramWeight ? `${item.gramWeight}g` : undefined,
    item.finishWidth ? `${item.finishWidth}mm` : undefined,
    item.finishDiameter ? `φ${item.finishDiameter}` : undefined,
    item.finishCoreDiameter ? `芯${item.finishCoreDiameter}` : undefined,
  ].filter(Boolean)
  return parts.length ? parts.join(' / ') : '-'
}

export function settleText(settleType?: number, settleDay?: number) {
  if (settleType === 1) return '次结'
  if (settleType === 2) return settleDay ? `月结 ${settleDay}日` : '月结'
  return '-'
}

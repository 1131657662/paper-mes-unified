import type {
  AvailableFinishVO,
  DeliveryDetail,
  DeliveryOriginalSourceItem,
  DeliveryProcessStepItem,
} from '../../../types/delivery'

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

export function deliveryDetailSpecText(item: DeliveryDetail) {
  const parts = [
    item.finishWidth ? `${item.finishWidth}mm` : undefined,
    item.finishDiameter ? `φ${item.finishDiameter}` : undefined,
    item.finishCoreDiameter ? `芯${item.finishCoreDiameter}` : undefined,
  ].filter(Boolean)
  return parts.length ? parts.join(' / ') : '-'
}

export function deliveryOriginalSnapshotText(item: DeliveryDetail) {
  const items = item.originalItems ?? []
  if (items.length > 0) {
    return items.map(originalSourceText).join('；')
  }
  return item.originalSummary || item.originalRollNos || '-'
}

export function deliveryProcessSnapshotText(item: DeliveryDetail) {
  const items = item.processStepItems ?? []
  if (items.length > 0) {
    return items.map(processStepText).join('；')
  }
  return item.processSummary || '-'
}

export function settleText(settleType?: number, settleDay?: number) {
  if (settleType === 1) return '次结'
  if (settleType === 2) return settleDay ? `月结 ${settleDay}日` : '月结'
  return '-'
}

function originalSourceText(item: DeliveryOriginalSourceItem) {
  const label = item.rowSort ? `母卷${item.rowSort}` : '母卷'
  const identity = [item.rollNo && `卷号${item.rollNo}`, item.extraNo && `编号${item.extraNo}`].filter(Boolean)
  const weight = item.actualWeight ?? item.totalWeight
  const spec = [
    item.paperName,
    item.actualGramWeight ? `${item.actualGramWeight}g` : item.gramWeight ? `${item.gramWeight}g` : undefined,
    item.actualWidth ? `${item.actualWidth}mm` : item.originalWidth ? `${item.originalWidth}mm` : undefined,
    weight != null ? formatKg(weight) : undefined,
  ].filter(Boolean)
  return `${label}｜${identity.length ? identity.join(' / ') : '无卷号'}｜${spec.join(' / ')}`
}

function processStepText(item: DeliveryProcessStepItem) {
  const parts = [
    item.knifeCount ? `${item.knifeCount}刀` : undefined,
    item.processWeight != null ? formatKg(item.processWeight) : undefined,
    item.unitPrice != null ? `单价${formatNumber(item.unitPrice)}` : undefined,
    item.lossWeight != null ? `损耗${formatKg(item.lossWeight)}` : undefined,
  ].filter(Boolean)
  return `${item.stepName || '加工'}${parts.length ? `（${parts.join(' / ')}）` : ''}`
}

function formatNumber(value: number) {
  return value.toLocaleString('zh-CN', {
    maximumFractionDigits: 2,
    minimumFractionDigits: 2,
  })
}

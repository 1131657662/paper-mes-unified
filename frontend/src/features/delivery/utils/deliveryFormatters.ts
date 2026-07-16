import type {
  AvailableFinishVO,
  DeliveryDetail,
  DeliveryOriginalSourceItem,
  DeliveryProcessStepItem,
} from '../../../types/delivery'
import {
  formatGram,
  formatKg as formatWeightKg,
  formatMm,
  formatNumber,
  formatTonFromKg,
} from '../../../utils/numberFormatters'

export function formatKg(value?: number) {
  return formatWeightKg(value)
}

export function formatTon(value?: number) {
  return formatTonFromKg(value)
}

export function availableFinishWeight(item: Pick<AvailableFinishVO, 'actualWeight' | 'remainingWeight'>) {
  return item.remainingWeight ?? item.actualWeight ?? 0
}

export function finishSpecText(item: AvailableFinishVO) {
  const parts = [
    item.gramWeight ? formatGram(item.gramWeight) : undefined,
    item.finishWidth ? formatMm(item.finishWidth) : undefined,
    item.finishDiameter ? `φ${item.finishDiameter} mm` : undefined,
    item.finishCoreDiameter ? `芯 ${item.finishCoreDiameter} mm` : undefined,
  ].filter(Boolean)
  return parts.length ? parts.join(' / ') : '-'
}

export function deliveryDetailSpecText(item: DeliveryDetail) {
  const parts = [
    item.finishWidth ? formatMm(item.finishWidth) : undefined,
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

export function deliveryOriginalIdentityText(item: DeliveryDetail) {
  const identities = (item.originalItems ?? []).map(originalIdentity).filter(Boolean)
  if (identities.length > 0) return identities.join('；')
  const legacyValue = item.originalRollNos?.trim()
  if (!legacyValue || looksLikeInternalId(legacyValue)) return '未记录卷号'
  return legacyValue
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
    item.actualGramWeight ? formatGram(item.actualGramWeight) : item.gramWeight ? formatGram(item.gramWeight) : undefined,
    item.actualWidth ? formatMm(item.actualWidth) : item.originalWidth ? formatMm(item.originalWidth) : undefined,
    weight != null ? formatKg(weight) : undefined,
    item.machineName ? `机台${item.machineName}` : undefined,
  ].filter(Boolean)
  return `${label}｜${identity.length ? identity.join(' / ') : '无卷号'}｜${spec.join(' / ')}`
}

function originalIdentity(item: DeliveryOriginalSourceItem) {
  const label = item.rowSort ? `母卷${item.rowSort}` : '母卷'
  const identity = item.rollNo || item.extraNo
  return identity ? `${label} ${identity}` : `${label} 未记录卷号`
}

function looksLikeInternalId(value: string) {
  return /^[0-9a-f]{32}$/i.test(value) || /^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(value)
}

function processStepText(item: DeliveryProcessStepItem) {
  const parts = [
    item.knifeCount ? `${item.knifeCount}刀` : undefined,
    item.processWeight != null ? formatKg(item.processWeight) : undefined,
    item.unitPrice != null ? `单价${formatNumber(item.unitPrice, 2)}` : undefined,
    item.lossWeight != null ? `损耗${formatKg(item.lossWeight)}` : undefined,
  ].filter(Boolean)
  return `${item.stepName || '加工'}${parts.length ? `（${parts.join(' / ')}）` : ''}`
}

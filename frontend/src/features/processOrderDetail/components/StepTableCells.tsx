import { Tag } from 'antd'
import type { ProcessOrderDetailVO, ProcessStep } from '../../../types/processOrder'
import { formatMoney, formatNumber } from '../orderDetailUtils'

const BILLING_MODE: Record<number, string> = {
  1: '标准计价',
  2: '指定数量',
  3: '固定金额',
  4: '免收',
}

export function renderRollCell(detail: ProcessOrderDetailVO | undefined, step: ProcessStep) {
  const roll = detail?.originalRolls?.find((item) => item.uuid === step.originalUuid)
  const gramWeight = roll?.actualGramWeight ?? roll?.gramWeight
  const width = roll?.actualWidth ?? roll?.originalWidth
  const weight = roll?.actualWeight ?? roll?.totalWeight ?? roll?.rollWeight
  const metadata = [
    roll?.paperName,
    gramWeight != null ? `${gramWeight}g` : undefined,
    width != null ? `${width}mm` : undefined,
    weight != null ? `来料 ${formatNumber(weight / 1000, 3)} t` : undefined,
    roll?.batchNo ? `批次 ${roll.batchNo}` : undefined,
  ].filter(Boolean).join(' · ')
  if (!roll) return <div className="order-detail-step-source-cell"><strong>母卷信息待补充</strong><span>{step.originalUuid || '-'}</span></div>
  const identifiers = `原始卷号 ${roll.rollNo || '-'} · 编号 ${roll.extraNo || '-'}`
  return (
    <div className="order-detail-step-source-cell">
      <div className="order-detail-step-source-cell__identity">
        <strong>第 {roll.rowSort == null ? '-' : String(roll.rowSort).padStart(2, '0')} 卷</strong>
        <span>{identifiers}</span>
      </div>
      <span>{metadata || '母卷规格待补充'}</span>
    </div>
  )
}

export function renderProcessingQuantity(step: ProcessStep): string {
  if (step.stepType === 1) {
    return step.knifeCount == null ? '-' : `${formatNumber(step.knifeCount, 0)} 刀`
  }
  if (step.stepType === 2) {
    return step.processWeight == null ? '-' : `${formatNumber(step.processWeight, 3)} t`
  }
  if (step.stepType === 3 || step.stepType === 4) {
    if (step.serviceQuantity == null) return step.billingMode === 4 ? '免费服务' : '-'
    const unit = step.billingBasis === 'PIECE' ? '件' : 't'
    return `${formatNumber(step.serviceQuantity, unit === '件' ? 0 : 3)} ${unit}`
  }
  return '-'
}

export function renderPricingBasisCell(step: ProcessStep) {
  if (isPendingServicePricing(step)) {
    return (
      <div className="order-detail-step-pricing-cell">
        <Tag color="orange">待定价</Tag>
        <span>{step.billingBasis === 'PIECE' ? '预计按件，数量自动取母卷件数' : '预计按吨，数量自动取母卷实重'}</span>
      </div>
    )
  }
  const mode = step.billingMode ?? 1
  const quantity = step.billingQuantity ?? step.standardQuantity
  return (
    <div className="order-detail-step-pricing-cell">
      <Tag color={mode === 1 ? 'default' : 'gold'}>{BILLING_MODE[mode] ?? BILLING_MODE[1]}</Tag>
      <span>{billingBasisText(step, mode, quantity)}</span>
    </div>
  )
}

export function renderUnitPriceCell(step: ProcessStep) {
  if (isPendingServicePricing(step)) return <Tag color="orange">待核定</Tag>
  if (step.billingUnitPrice == null) return formatMoney(step.unitPrice)
  return (
    <div className="order-detail-step-unit-price-cell">
      <strong>{formatMoney(step.billingUnitPrice)}</strong>
      <span>标准 {formatMoney(step.unitPrice)}</span>
    </div>
  )
}

export function renderAmountCell(step: ProcessStep) {
  if (isPendingServicePricing(step)) return <Tag color="orange">待定价</Tag>
  if (step.stepAmount == null) return '-'
  const standardAmount = step.standardStepAmount ?? step.stepAmount
  return (
    <div className="order-detail-step-amount-cell">
      <strong>{formatMoney(step.stepAmount)}</strong>
      <span>标准 {formatMoney(standardAmount)}</span>
    </div>
  )
}

function isPendingServicePricing(step: ProcessStep): boolean {
  return (step.stepType === 3 || step.stepType === 4)
    && (step.billingMode ?? 1) === 1
    && step.unitPrice == null
    && step.billingUnitPrice == null
}

function billingBasisText(step: ProcessStep, mode: number, quantity?: number): string {
  if (mode === 3) return '按核定固定金额'
  if (mode === 4) return '本工序免收'
  if (quantity == null) return '按实际加工量'
  const unit = step.stepType === 2 ? 't' : step.billingBasis === 'PIECE' ? '件' : step.stepType === 1 ? '刀' : 't'
  return `计费 ${formatNumber(quantity, unit === 't' ? 3 : 0)} ${unit}`
}

export function renderAdjustmentCell(step: ProcessStep) {
  if (!step.pricingAdjustmentAmount && !step.pricingAdjustmentReason) return '-'
  return (
    <div className="order-detail-step-adjustment-cell">
      <strong>{formatMoney(step.pricingAdjustmentAmount)}</strong>
      <span>{step.pricingAdjustmentReason || '已核定'}</span>
    </div>
  )
}

import type { ProcessStepDTO } from '../../api/processOrder'
import type { ProcessStep } from '../../types/processOrder'

type ServiceValues = Omit<Partial<ProcessStepDTO>, 'billingMode'> & {
  billingMode?: number
  fixedAmountScope?: 'TOTAL' | 'EACH'
}

export function serviceStepName(stepType?: number, stepName?: string) {
  if (stepName?.trim()) return stepName.trim()
  if (stepType === 3) return '剥损整理'
  if (stepType === 4) return '重新包装'
  return '附加工艺'
}

export function servicePricingSummary(values?: ServiceValues) {
  if (!values?.stepType) return '尚未选择工序'
  const name = serviceStepName(values.stepType, values.stepName)
  if (values.billingMode === 4) return `${name} · 免费`
  if (values.billingMode === 3) {
    return `${name} · 固定 ${money(values.billingAmount)}`
  }
  const basis = values.billingBasis === 'TON' ? '吨' : '件'
  if (values.billingMode === 0 || values.unitPrice == null) return `${name} · 按${basis}待定价`
  return `${name} · ${money(values.unitPrice)}/${basis}`
}

export function serviceStepMatchesDraft(draft: ServiceValues, saved?: ProcessStep) {
  if (!saved || draft.stepType !== saved.stepType) return false
  const mode = draft.billingMode === 0 ? 1 : draft.billingMode
  return same(mode, saved.billingMode)
    && same(draft.machineUuid, saved.machineUuid)
    && sameText(
      draft.stepName || serviceStepName(draft.stepType),
      saved.stepName || serviceStepName(saved.stepType),
    )
    && sameText(draft.remark, saved.remark)
    && pricingMatches({ draft, saved, mode })
    && (mode !== 3 || (draft.fixedAmountScope ?? 'TOTAL') === 'TOTAL')
}

function pricingMatches(options: { draft: ServiceValues; saved: ProcessStep; mode?: number }) {
  const { draft, saved, mode } = options
  if (mode === 4) return true
  if (mode === 3) return sameNumber(draft.billingAmount, saved.billingAmount)
  return same(draft.billingBasis, saved.billingBasis)
    && sameNumber(draft.unitPrice, saved.unitPrice ?? saved.billingUnitPrice)
}

function money(value?: number) {
  return value == null ? '金额待填写' : `¥${Number(value).toFixed(2)}`
}

function same(left: unknown, right: unknown) {
  return (left ?? null) === (right ?? null)
}

function sameText(left?: string, right?: string) {
  return (left?.trim() || '') === (right?.trim() || '')
}

function sameNumber(left?: number, right?: number) {
  if (left == null || right == null) return left == null && right == null
  return Math.abs(Number(left) - Number(right)) < 0.000001
}

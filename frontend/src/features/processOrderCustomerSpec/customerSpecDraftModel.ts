import type {
  CustomerWeightMode,
  CustomerWeightZeroPolicy,
  FinishCustomerRevisionRequest,
  FinishCustomerRevisionRequestItem,
  FinishCustomerSpec,
} from './customerSpecTypes'

export interface CustomerSpecDraft extends FinishCustomerSpec {
  weightOperand?: number
  formulaExpression?: string
  zeroPolicy: CustomerWeightZeroPolicy
}

export interface BulkSpecificationValues {
  paperName?: string
  gramWeight?: number
  finishWidth?: number
}

export interface WeightRule {
  mode: CustomerWeightMode
  operand?: number
  formula?: string
  skipZero: boolean
}

interface PreviewRowsOptions {
  rows: CustomerSpecDraft[]
  selected: string[]
  rule: WeightRule
  applyPendingRule: boolean
}

export const STANDARD_WEIGHT_FORMULA = 'physicalWeight * (customerGsm / physicalGsm) * (customerWidth / physicalWidth)'

export function createCustomerSpecDrafts(items: FinishCustomerSpec[]): CustomerSpecDraft[] {
  return items.map((item) => ({ ...item, zeroPolicy: 'SKIP' }))
}

export function applyBulkSpecification(
  rows: CustomerSpecDraft[], selected: string[], values: BulkSpecificationValues,
) {
  const keys = new Set(selected)
  return rows.map((row) => keys.has(row.finishUuid) ? applySpecification(row, values) : row)
}

export function fillSelectedFromFirst(rows: CustomerSpecDraft[], selected: string[]) {
  const source = rows.find((row) => selected.includes(row.finishUuid))
  if (!source) return rows
  return applyBulkSpecification(rows, selected, customerValues(source))
}

export function applyToSamePhysicalSpec(rows: CustomerSpecDraft[], selected: string[]) {
  const source = rows.find((row) => selected.includes(row.finishUuid))
  if (!source) return rows
  const sameSpec = rows.filter((row) => physicalKey(row) === physicalKey(source)).map((row) => row.finishUuid)
  return applyBulkSpecification(rows, sameSpec, customerValues(source))
}

export function applyWeightRule(rows: CustomerSpecDraft[], selected: string[], rule: WeightRule) {
  const keys = new Set(selected)
  return rows.map((row) => keys.has(row.finishUuid) ? applyRule(row, rule) : row)
}

export function prepareCustomerSpecPreviewRows(options: PreviewRowsOptions) {
  if (!options.applyPendingRule) return options.rows
  return applyWeightRule(options.rows, options.selected, options.rule)
}

export function isWeightRuleReady(rule: WeightRule) {
  if (['FIXED', 'DELTA', 'RATIO'].includes(rule.mode)) return rule.operand != null
  if (rule.mode === 'FORMULA') return Boolean(rule.formula?.trim())
  return true
}

export function updateCustomerSpecDraft(
  rows: CustomerSpecDraft[], finishUuid: string, values: Partial<CustomerSpecDraft>,
) {
  return rows.map((row) => row.finishUuid === finishUuid ? { ...row, ...values } : row)
}

export function buildFinishCustomerRevisionRequest(
  orderVersion: number, reason: string, requestId: string,
  rows: CustomerSpecDraft[], selected: string[],
): FinishCustomerRevisionRequest {
  const keys = new Set(selected)
  return {
    requestId, expectedOrderVersion: orderVersion, reason: reason.trim(),
    items: rows.filter((row) => keys.has(row.finishUuid)).map(toRequestItem),
  }
}

function toRequestItem(row: CustomerSpecDraft): FinishCustomerRevisionRequestItem {
  return {
    finishUuid: row.finishUuid, expectedVersion: row.finishVersion,
    customerPaperName: row.customerPaperName?.trim(),
    customerGramWeight: row.customerGramWeight,
    customerFinishWidth: row.customerFinishWidth,
    customerDisplayWeight: row.customerDisplayWeight,
    calculationMode: row.calculationMode,
    weightOperand: row.weightOperand,
    formulaExpression: row.formulaExpression,
    roundingScale: 0, roundingMode: 'HALF_UP', zeroPolicy: row.zeroPolicy,
  }
}

function applySpecification(row: CustomerSpecDraft, values: BulkSpecificationValues) {
  return {
    ...row,
    customerPaperName: values.paperName?.trim() || row.customerPaperName,
    customerGramWeight: values.gramWeight ?? row.customerGramWeight,
    customerFinishWidth: values.finishWidth ?? row.customerFinishWidth,
  }
}

function applyRule(row: CustomerSpecDraft, rule: WeightRule): CustomerSpecDraft {
  const fixedWeight = rule.mode === 'FIXED' ? rule.operand : row.customerDisplayWeight
  return {
    ...row, calculationMode: rule.mode, customerDisplayWeight: fixedWeight,
    weightOperand: ['DELTA', 'RATIO'].includes(rule.mode) ? rule.operand : undefined,
    formulaExpression: rule.mode === 'FORMULA' ? rule.formula : undefined,
    zeroPolicy: rule.skipZero ? 'SKIP' : 'ERROR',
  }
}

const customerValues = (row: CustomerSpecDraft) => ({ paperName: row.customerPaperName, gramWeight: row.customerGramWeight, finishWidth: row.customerFinishWidth })
const physicalKey = (row: CustomerSpecDraft) => [row.physicalPaperName, row.physicalGramWeight, row.physicalFinishWidth].join('|')

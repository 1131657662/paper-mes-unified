import type { BulkSpecificationValues, WeightRule } from '../processOrderCustomerSpec/customerSpecDraftModel'
import { parsePastedCustomerSpecs } from '../processOrderCustomerSpec/customerSpecPasteModel'
import type {
  DeliveryCustomerRevisionRequest,
  DeliveryCustomerRevisionRequestItem,
  DeliveryCustomerSpec,
} from './deliveryCustomerSpecTypes'

export interface DeliveryCustomerSpecDraft extends DeliveryCustomerSpec {
  weightOperand?: number
  formulaExpression?: string
  zeroPolicy: 'SKIP' | 'ERROR'
}

export function createDeliveryCustomerDrafts(items: DeliveryCustomerSpec[]) {
  return items.map((item): DeliveryCustomerSpecDraft => ({ ...item, zeroPolicy: 'SKIP' }))
}

export function applyDeliveryBulkSpecification(
  rows: DeliveryCustomerSpecDraft[], selected: string[], values: BulkSpecificationValues,
) {
  const keys = new Set(selected)
  return rows.map((row): DeliveryCustomerSpecDraft => keys.has(row.deliveryDetailUuid) ? {
    ...row,
    customerPaperName: values.paperName?.trim() || row.customerPaperName,
    customerGramWeight: values.gramWeight ?? row.customerGramWeight,
    customerFinishWidth: values.finishWidth ?? row.customerFinishWidth,
  } : row)
}

export function fillDeliverySelected(rows: DeliveryCustomerSpecDraft[], selected: string[]) {
  const source = rows.find((row) => selected.includes(row.deliveryDetailUuid))
  if (!source) return rows
  return applyDeliveryBulkSpecification(rows, selected, customerValues(source))
}

export function applyDeliverySamePhysical(rows: DeliveryCustomerSpecDraft[], selected: string[]) {
  const source = rows.find((row) => selected.includes(row.deliveryDetailUuid))
  if (!source) return rows
  const keys = rows.filter((row) => physicalKey(row) === physicalKey(source))
    .map((row) => row.deliveryDetailUuid)
  return applyDeliveryBulkSpecification(rows, keys, customerValues(source))
}

export function applyDeliveryWeightRule(
  rows: DeliveryCustomerSpecDraft[], selected: string[], rule: WeightRule,
) {
  const keys = new Set(selected)
  return rows.map((row): DeliveryCustomerSpecDraft => keys.has(row.deliveryDetailUuid) ? {
    ...row,
    calculationMode: rule.mode,
    customerDisplayWeight: rule.mode === 'FIXED' ? rule.operand : row.customerDisplayWeight,
    weightOperand: ['DELTA', 'RATIO'].includes(rule.mode) ? rule.operand : undefined,
    formulaExpression: rule.mode === 'FORMULA' ? rule.formula : undefined,
    zeroPolicy: rule.skipZero ? 'SKIP' : 'ERROR',
  } : row)
}

export function prepareDeliveryCustomerPreviewRows(
  rows: DeliveryCustomerSpecDraft[], selected: string[], rule: WeightRule, applyPendingRule: boolean,
) {
  return applyPendingRule ? applyDeliveryWeightRule(rows, selected, rule) : rows
}

export function updateDeliveryCustomerDraft(
  rows: DeliveryCustomerSpecDraft[], uuid: string, values: Partial<DeliveryCustomerSpecDraft>,
) {
  return rows.map((row) => row.deliveryDetailUuid === uuid ? { ...row, ...values } : row)
}

export function applyDeliveryPaste(rows: DeliveryCustomerSpecDraft[], text: string) {
  const pasted = parsePastedCustomerSpecs(text)
  const byRoll = new Map(pasted.filter((item) => item.finishRollNo).map((item) => [item.finishRollNo, item]))
  let index = 0
  return rows.map((row) => {
    const item = (row.finishRollNo ? byRoll.get(row.finishRollNo) : undefined)
      ?? (byRoll.size ? undefined : pasted[index++])
    if (!item) return row
    return {
      ...row,
      customerPaperName: item.paperName ?? row.customerPaperName,
      customerGramWeight: item.gramWeight ?? row.customerGramWeight,
      customerFinishWidth: item.finishWidth ?? row.customerFinishWidth,
      customerDisplayWeight: item.displayWeight ?? row.customerDisplayWeight,
      calculationMode: item.displayWeight == null ? row.calculationMode : 'MANUAL',
    }
  })
}

export function buildDeliveryCustomerRevisionRequest(
  deliveryVersion: number, reason: string, requestId: string,
  rows: DeliveryCustomerSpecDraft[], selected: string[],
): DeliveryCustomerRevisionRequest {
  const keys = new Set(selected)
  return {
    requestId, expectedDeliveryVersion: deliveryVersion, reason: reason.trim(),
    items: rows.filter((row) => keys.has(row.deliveryDetailUuid)).map(toRequestItem),
  }
}

function toRequestItem(row: DeliveryCustomerSpecDraft): DeliveryCustomerRevisionRequestItem {
  return {
    deliveryDetailUuid: row.deliveryDetailUuid, expectedDetailVersion: row.detailVersion,
    customerPaperName: row.customerPaperName?.trim(), customerGramWeight: row.customerGramWeight,
    customerFinishWidth: row.customerFinishWidth, customerDisplayWeight: row.customerDisplayWeight,
    calculationMode: row.calculationMode, weightOperand: row.weightOperand,
    formulaExpression: row.formulaExpression, roundingScale: 0, roundingMode: 'HALF_UP',
    zeroPolicy: row.zeroPolicy, customerRemark: row.customerRemark?.trim(),
  }
}

const customerValues = (row: DeliveryCustomerSpecDraft) => ({ paperName: row.customerPaperName, gramWeight: row.customerGramWeight, finishWidth: row.customerFinishWidth })
const physicalKey = (row: DeliveryCustomerSpecDraft) => [row.physicalPaperName, row.physicalGramWeight, row.physicalFinishWidth].join('|')

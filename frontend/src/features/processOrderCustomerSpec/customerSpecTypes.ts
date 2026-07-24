export type CustomerWeightMode = 'KEEP' | 'FIXED' | 'DELTA' | 'RATIO' | 'FORMULA' | 'MANUAL'
export type CustomerWeightZeroPolicy = 'SKIP' | 'ERROR' | 'USE_ZERO'
export type CustomerWeightRoundingMode = 'HALF_UP' | 'UP' | 'DOWN'

export interface FinishCustomerSpec {
  finishUuid: string
  finishRollNo?: string
  rowSort?: number
  finishVersion: number
  physicalPaperName?: string
  physicalGramWeight?: number
  physicalFinishWidth?: number
  physicalWeight?: number
  previousCustomerPaperName?: string
  previousCustomerGramWeight?: number
  previousCustomerFinishWidth?: number
  previousCustomerDisplayWeight?: number
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerDisplayWeight?: number
  calculationMode: CustomerWeightMode
  specificationChanged: boolean
  weightChanged: boolean
  valid: boolean
  error?: string
}

export interface FinishCustomerRevisionRequestItem {
  finishUuid: string
  expectedVersion: number
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerDisplayWeight?: number
  calculationMode: CustomerWeightMode
  weightOperand?: number
  formulaExpression?: string
  formulaVariables?: Record<string, number>
  roundingScale?: number
  roundingMode?: CustomerWeightRoundingMode
  zeroPolicy?: CustomerWeightZeroPolicy
  remark?: string
}

export interface FinishCustomerRevisionRequest {
  requestId: string
  expectedOrderVersion: number
  reason: string
  items: FinishCustomerRevisionRequestItem[]
}

export interface FinishCustomerRevisionPreview {
  orderUuid: string
  orderNo?: string
  orderVersion: number
  nextRevisionNo: number
  itemCount: number
  validItemCount: number
  physicalTotalWeight: number
  customerTotalWeight: number
  differenceWeight: number
  hasErrors: boolean
  items: FinishCustomerSpec[]
}

export interface FinishCustomerRevisionSummary {
  uuid: string
  orderUuid: string
  revisionNo: number
  sourceStage?: string
  reason: string
  itemCount: number
  customerTotalWeight?: number
  operator?: string
  createdAt?: string
}

export interface FinishCustomerRevisionItem {
  finishUuid: string
  finishRollNo?: string
  physicalPaperName?: string
  physicalGramWeight?: number
  physicalFinishWidth?: number
  physicalWeightSnapshot?: number
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerDisplayWeight?: number
  calculationMode: CustomerWeightMode
  weightOperand?: number
  formulaExpression?: string
  formulaVariables?: Record<string, number>
  roundingScale?: number
  roundingMode?: CustomerWeightRoundingMode
  zeroPolicy?: CustomerWeightZeroPolicy
  remark?: string
}

export interface FinishCustomerRevisionDetail extends FinishCustomerRevisionSummary {
  sourceStage?: string
  items: FinishCustomerRevisionItem[]
}

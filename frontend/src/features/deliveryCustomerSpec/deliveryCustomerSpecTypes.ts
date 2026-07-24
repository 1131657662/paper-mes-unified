import type {
  CustomerWeightMode,
  CustomerWeightRoundingMode,
  CustomerWeightZeroPolicy,
} from '../processOrderCustomerSpec/customerSpecTypes'

export interface DeliveryCustomerSpec {
  deliveryDetailUuid: string
  detailVersion: number
  finishUuid: string
  finishRollNo?: string
  orderUuid?: string
  orderNo?: string
  physicalPaperName?: string
  physicalGramWeight?: number
  physicalFinishWidth?: number
  physicalDeliveryWeight?: number
  previousCustomerPaperName?: string
  previousCustomerGramWeight?: number
  previousCustomerFinishWidth?: number
  previousCustomerDisplayWeight?: number
  customerPaperName?: string
  customerGramWeight?: number
  customerFinishWidth?: number
  customerDisplayWeight?: number
  customerRemark?: string
  calculationMode: CustomerWeightMode
  valueSource: 'PHYSICAL' | 'FINISH_DEFAULT' | 'DELIVERY_REVISION' | 'HISTORICAL_BASELINE' | 'DRAFT'
  specificationChanged: boolean
  weightChanged: boolean
  valid: boolean
  error?: string
}

export type DeliveryCustomerRevisionKind =
  | 'LIVE_FINISH'
  | 'SYSTEM_BASELINE'
  | 'USER_REVISION'
  | 'HISTORICAL_BASELINE'

export interface DeliveryCustomerRevisionRequestItem {
  deliveryDetailUuid: string
  expectedDetailVersion: number
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
  customerRemark?: string
}

export interface DeliveryCustomerRevisionRequest {
  requestId: string
  expectedDeliveryVersion: number
  reason: string
  items: DeliveryCustomerRevisionRequestItem[]
}

export interface DeliveryCustomerRevisionPreview {
  deliveryUuid: string
  deliveryNo: string
  deliveryVersion: number
  deliveryStatus: number
  currentRevisionNo: number
  currentRevisionKind: DeliveryCustomerRevisionKind
  nextRevisionNo: number
  itemCount: number
  validItemCount: number
  physicalTotalWeight: number
  customerTotalWeight: number
  differenceWeight: number
  hasErrors: boolean
  items: DeliveryCustomerSpec[]
}

export interface DeliveryCustomerRevisionSummary {
  uuid: string
  deliveryUuid: string
  revisionNo: number
  reason: string
  itemCount: number
  customerTotalWeight?: number
  operator?: string
  createdAt?: string
}

export interface DeliveryCustomerRevisionItem {
  deliveryDetailUuid: string
  finishUuid: string
  finishRollNo?: string
  physicalPaperName?: string
  physicalGramWeight?: number
  physicalFinishWidth?: number
  physicalDeliveryWeight?: number
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
  customerRemark?: string
}

export interface DeliveryCustomerRevisionDetail extends DeliveryCustomerRevisionSummary {
  items: DeliveryCustomerRevisionItem[]
}

export type DeliveryDocumentView = 'customer' | 'physical' | 'trace'

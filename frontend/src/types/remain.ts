export type RemainRegistrationStatus = 'ACTIVE' | 'PARTIAL_ROLLED_BACK' | 'FULL_ROLLED_BACK'
export type RemainPriceStatus = 'PRICE_PENDING' | 'CONFIRMED' | 'VOIDED'
export type RemainAdjustmentStatus = 'PENDING' | 'APPLIED' | 'REVERSED' | 'CANCELLED'
export type RemainAdjustmentTarget = 'PENDING' | 'NEXT_SETTLEMENT' | 'CUSTOMER_CREDIT' | 'REFUND'
export type RemainRefundStatus = 'REQUESTED' | 'APPROVED' | 'PAID' | 'CANCELLED'

export interface RemainRegistrationLine {
  uuid?: string
  registrationLineUuid?: string
  sourceFinishRollUuid: string
  sourceSystemWeight?: number
  transferredSystemWeight: number
  rolledBackSystemWeight?: number
  processedSystemWeight?: number
  currentOwnWeight?: number
  amount?: number
  appliedAmount?: number
  appliedWeight?: number
  status?: RemainRegistrationStatus
}

export interface RemainRegistration {
  uuid: string
  registrationNo: string
  requestId: string
  orderUuid: string
  customerUuid: string
  registrationDate: string
  confirmationName: string
  confirmationChannel: string
  confirmationAt: string
  confirmationEvidence: string
  status: RemainRegistrationStatus
  priceStatus: RemainPriceStatus
  priceVersion: number
  pricingBasis?: string
  priceConfirmedAt?: string
  priceConfirmedBy?: string
  totalTransferredWeight: number
  totalRolledBackWeight: number
  totalProcessedWeight: number
  totalAmount: number
  lines?: RemainRegistrationLine[]
}

export interface RemainRegistrationQuery {
  orderUuid?: string
  customerUuid?: string
}

export interface CreateRemainRegistrationLine {
  sourceFinishRollUuid: string
  transferredSystemWeight: number
}

export interface CreateRemainRegistrationRequest {
  requestId: string
  orderUuid: string
  confirmationName: string
  confirmationChannel: string
  confirmationAt: string
  confirmationEvidence: string
  remark?: string
  lines: CreateRemainRegistrationLine[]
}

export interface ConfirmRemainPriceRequest {
  requestId: string
  pricingBasis: string
  totalAmount: number
}

export interface RollbackRemainLine {
  registrationLineUuid: string
  rollbackWeight: number
}

export interface RollbackRemainRequest {
  requestId: string
  reason: string
  lines: RollbackRemainLine[]
}

export interface RemainInventory {
  lotUuid: string
  registrationUuid: string
  registrationNo: string
  registrationLineUuid: string
  sourceFinishRollUuid: string
  customerUuid: string
  warehouseUuid?: string
  currentWeight: number
  status: string
  priceStatus: RemainPriceStatus
}

export interface RemainInventoryQuery {
  registrationUuid?: string
  customerUuid?: string
  availableOnly?: boolean
}

export interface RemainAdjustmentLine {
  registrationLineUuid: string
  amount: number
  weight: number
}

export interface RemainAdjustment {
  uuid: string
  adjustmentNo: string
  registrationUuid: string
  sourceSettleUuid?: string
  targetSettleUuid?: string
  customerUuid: string
  targetType: RemainAdjustmentTarget
  status: RemainAdjustmentStatus
  amount: number
  weight: number
  reason?: string
  lines?: RemainAdjustmentLine[]
}

export interface RemainAdjustmentCreateRequest {
  requestId: string
  sourceSettleUuid: string
}

export interface RemainAdjustmentCancelRequest {
  requestId: string
  reason: string
}

export interface RemainAdjustmentNextSettlementRequest {
  requestId: string
  settleUuid: string
}

export interface RemainCreditRequest {
  requestId: string
}

export interface RemainCreditReverseRequest {
  requestId: string
  reason: string
}

export interface RemainRefund {
  uuid: string
  refundNo: string
  adjustmentUuid: string
  customerUuid: string
  amount: number
  weight: number
  status: RemainRefundStatus
  paymentReference?: string
  reason?: string
  approvedAt?: string
  paidAt?: string
}

export interface RemainRefundCreateRequest {
  requestId: string
  reason?: string
}

export interface RemainRefundDecisionRequest {
  requestId: string
  reason?: string
  paymentReference?: string
}

export type RemainSaleKind = 'SALE' | 'REVERSAL'
export type RemainSaleStatus = 'CONFIRMED' | 'VOIDED'

export interface RemainSaleLine {
  uuid?: string
  saleUuid?: string
  lotUuid: string
  registrationLineUuid?: string
  systemWeight: number
  amount?: number
}

export interface RemainSale {
  uuid: string
  saleNo: string
  requestId: string
  saleKind: RemainSaleKind
  reversalOfUuid?: string
  processDate: string
  warehouseUuid?: string
  pricingMode: string
  systemWeight: number
  actualWeight?: number
  unitPrice?: number
  calculatedAmount: number
  receivedAmount: number
  buyerName?: string
  vehicleNo?: string
  weighingTicketNo?: string
  weighingEvidence?: string
  status: RemainSaleStatus
  reason?: string
}

export interface RemainSaleLineInput {
  lotUuid: string
  systemWeight: number
}

export interface RemainSaleCreateRequest {
  requestId: string
  processDate: string
  warehouseUuid?: string
  pricingMode: string
  actualWeight?: number
  unitPrice?: number
  totalAmount?: number
  receivedAmount: number
  buyerName?: string
  vehicleNo?: string
  weighingTicketNo?: string
  weighingEvidence?: string
  reason?: string
  lines: RemainSaleLineInput[]
}

export interface RemainSaleReverseRequest {
  requestId: string
  reason: string
}

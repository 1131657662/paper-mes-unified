/** Processing-order payment terms: 1 per-order settlement, 2 monthly settlement. */
export type ProcessOrderSettlementMode = 1 | 2

export function isProcessOrderSettlementMode(value: unknown): value is ProcessOrderSettlementMode {
  return value === 1 || value === 2
}

/** Settlement document origin: 1 single order, 2 monthly batch, 3 selected merge. */
export type SettlementDocumentMode = 1 | 2 | 3

/** Delivery settlement risk returned by the API. */
export type DeliverySettlementRisk = 'NONE' | 'UNSETTLED_CASH'

export interface DeliverySettlementRiskSource {
  settlementRiskState?: DeliverySettlementRisk
  /** @deprecated Compatibility field for clients running before risk states were introduced. */
  settlementRisk?: boolean
}

export function hasDeliverySettlementRisk(source: DeliverySettlementRiskSource): boolean {
  if (source.settlementRiskState) return source.settlementRiskState === 'UNSETTLED_CASH'
  return source.settlementRisk === true
}

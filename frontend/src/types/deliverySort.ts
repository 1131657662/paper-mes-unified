export type DeliveryDetailSortField =
  | 'orderNo'
  | 'finishRollNo'
  | 'paperName'
  | 'gramWeight'
  | 'spec'
  | 'actualWeight'
  | 'outWeight'
  | 'remainingWeight'
  | 'originalSummary'
  | 'remark'
  | 'actualRemark'

export type DeliverySortDirection = 'asc' | 'desc'

export interface DeliverySortSpec {
  field: DeliveryDetailSortField
  direction: DeliverySortDirection
}

export type DeliveryCustomerSortField =
  | 'finishRollNo'
  | 'customerPaperName'
  | 'customerSpecification'
  | 'customerDisplayWeight'
  | 'orderNo'
  | 'customerRemark'
  | 'sourceMotherRoll'

export interface DeliveryCustomerSortSpec {
  field: DeliveryCustomerSortField
  direction: DeliverySortDirection
}

export interface DeliveryExportSortChains {
  physical: DeliverySortSpec[]
  customer: DeliveryCustomerSortSpec[]
  trace: DeliveryCustomerSortSpec[]
}

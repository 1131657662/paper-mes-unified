import type { ReportQueryExecutionMetaVO } from './report'

export interface ReportSettlementOverviewVO {
  totalDocuments: number
  pendingDocuments: number
  partialDocuments: number
  overdueDocuments: number
  totalAmount: number
  receivedAmount: number
  unreceivedAmount: number
  overdueAmount: number
}

export interface ReportSettlementDimensionVO {
  dimensionKey: string
  dimensionName: string
  documentCount: number
  totalAmount: number
  receivedAmount: number
  unreceivedAmount: number
}

export interface ReportSettlementAnalysisVO {
  topicCode: 'settlement'
  overview: ReportSettlementOverviewVO
  monthlyTrend: ReportSettlementDimensionVO[]
  customerBreakdown: ReportSettlementDimensionVO[]
  asOf: string
  execution: ReportQueryExecutionMetaVO
}

export interface ReportCollectionDimensionVO {
  dimensionKey: string
  dimensionName: string
  recordCount: number
  settledAmount: number
  cashAmount: number
  nonCashAmount: number
  scrapWeight: number
}

export interface ReportCollectionAnalysisVO {
  topicCode: 'collection'
  overview: {
    recordCount: number
    cashRecordCount: number
    scrapRecordCount: number
    discountRecordCount: number
    settledAmount: number
    cashAmount: number
    scrapOffsetAmount: number
    discountAmount: number
    scrapWeight: number
  }
  monthlyTrend: ReportCollectionDimensionVO[]
  customerBreakdown: ReportCollectionDimensionVO[]
  asOf: string
  execution: ReportQueryExecutionMetaVO
}

export interface ReportInventoryDimensionVO {
  dimensionKey: string
  dimensionName: string
  rollCount: number
  totalWeight: number
  lockedWeight: number
}

export interface ReportInventoryAnalysisVO {
  topicCode: 'inventory'
  timelineMode: 'CURRENT_STOCK_BY_STOCK_IN_MONTH'
  overview: {
    rollCount: number
    availableRollCount: number
    lockedRollCount: number
    exceptionRollCount: number
    totalWeight: number
    lockedWeight: number
  }
  stockInCohorts: ReportInventoryDimensionVO[]
  warehouseBreakdown: ReportInventoryDimensionVO[]
  asOf: string
  execution: ReportQueryExecutionMetaVO
}

export interface ReportDeliveryDimensionVO {
  dimensionKey: string
  dimensionName: string
  documentCount: number
  rollCount: number
  totalWeight: number
  completedWeight: number
}

export interface ReportDeliveryAnalysisVO {
  topicCode: 'delivery'
  overview: {
    documentCount: number
    pendingDocuments: number
    completedDocuments: number
    rollCount: number
    totalWeight: number
    pendingWeight: number
    completedWeight: number
  }
  monthlyTrend: ReportDeliveryDimensionVO[]
  warehouseBreakdown: ReportDeliveryDimensionVO[]
  asOf: string
  execution: ReportQueryExecutionMetaVO
}

export type ReportOperationalTopicCode = 'settlement' | 'collection' | 'inventory' | 'delivery'
export type ReportOperationalAnalysisVO =
  | ReportSettlementAnalysisVO
  | ReportCollectionAnalysisVO
  | ReportInventoryAnalysisVO
  | ReportDeliveryAnalysisVO

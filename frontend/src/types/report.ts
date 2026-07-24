import type { PageQuery } from './common'

export type {
  ReportDimensionAnalysisVO, ReportPageAnalysisVO, ReportProductionAnalysisVO,
  ReportQualityLossAnalysisVO, ReportTopicAnalysisVO,
} from './reportAnalysis'

export interface ReportQuery {
  metricReleaseUuid?: string
  dateFrom?: string
  dateTo?: string
  customerUuid?: string
  paperName?: string
  mainStepType?: number
  processStepType?: number
  processMode?: number
  machineUuid?: string
  settleType?: number
  isInvoice?: number
  orderStatus?: number
  dimension?: ReportDimension
}

export type ReportSourcePath =
  | '/reports/overview'
  | '/reports/production'
  | '/reports/quality-loss'
  | '/reports/settlement'
  | '/reports/collection'
  | '/reports/inventory'
  | '/reports/delivery'
  | '/reports/explorer'

export interface ReportExportRequest {
  query: ReportQuery
  reportPath: ReportSourcePath
}

export interface ReportDetailQuery extends ReportQuery, PageQuery {}

export interface ReportMetricItemVO {
  metricUuid: string
  metricCode: string
  metricName: string
  description: string
  valueType: 'INTEGER' | 'DECIMAL' | 'MONEY' | 'PERCENT'
  unitCode: string
  displayScale: number
  metricVersionUuid: string
  versionNo: number
  definitionChecksum: string
}

export interface ReportMetricContextVO {
  releaseUuid: string
  releaseCode: string
  releaseName: string
  releaseChecksum: string
  publishedAt: string
  asOf: string
  metrics: ReportMetricItemVO[]
}

export interface ReportQueryExecutionMetaVO {
  queryId: string
  queryHash: string
  metricReleaseUuid: string
  metricVersionMap: Record<string, string>
  dataAsOf: string
  sourceWatermark: string
  consistencyMode: 'LIVE_DB_READ' | 'MATERIALIZED'
  coverage: 'LIVE_ONLY' | 'MATERIALIZED'
  warnings: string[]
  sectionStatuses: Record<'overview' | 'dimensions' | 'details', 'READY' | 'FAILED'>
}

export interface ReportQuerySnapshotVO extends ReportQueryExecutionMetaVO {
  querySnapshotUuid: string
  expiresAt: string
  scopeHash: string
}

export type ReportMetricReleaseStatus = 1 | 2 | 3

export interface ReportMetricReleaseSummaryVO {
  releaseUuid: string
  releaseCode: string
  releaseName: string
  releaseStatus: ReportMetricReleaseStatus
  releaseChecksum?: string
  metricCount: number
  publishedAt?: string
  publishedBy?: string
  retiredAt?: string
  retiredBy?: string
  createTime: string
  asOf: string
}

export interface ReportMetricVersionAuditVO {
  metricUuid: string
  metricCode: string
  metricName: string
  description: string
  valueType: ReportMetricItemVO['valueType']
  unitCode: string
  displayScale: number
  displayOrder: number
  metricVersionUuid: string
  versionNo: number
  implementationKey: string
  definitionJson: string
  definitionChecksum: string
  versionStatus: 1 | 2
  lockedAt?: string
  lockedBy?: string
}

export interface ReportMetricReleaseDetailVO {
  release: ReportMetricReleaseSummaryVO
  metrics: ReportMetricVersionAuditVO[]
}

export type ReportDimension =
  | 'month'
  | 'customer'
  | 'paper'
  | 'process'
  | 'machine'
  | 'invoice'
  | 'settleType'
  | 'status'

export interface ReportOverviewVO {
  orderCount: number
  originalRollCount: number
  finishRollCount: number
  originalWeight: number
  finishWeight: number
  lossWeight: number
  lossRatio: number
  knifeCount: number
  sawAmount: number
  rewindAmount: number
  processAmount: number
  extraAmount: number
  totalAmount: number
  settledAmount: number
  pendingSettleAmount: number
  receivedAmount: number
  cashReceivedAmount: number
  scrapOffsetAmount: number
  unreceivedAmount: number
}

export interface ReportDimensionVO {
  dimensionKey: string
  dimensionName: string
  orderCount: number
  originalRollCount: number
  finishRollCount: number
  originalWeight: number
  finishWeight: number
  lossWeight: number
  lossRatio: number
  knifeCount: number
  sawAmount: number
  rewindAmount: number
  processAmount: number
  extraAmount: number
  totalAmount: number
  settledAmount: number
  pendingSettleAmount: number
  receivedAmount: number
  cashReceivedAmount: number
  scrapOffsetAmount: number
  unreceivedAmount: number
}

export interface ReportDetailVO {
  orderUuid: string
  orderNo: string
  orderDate: string
  accountingDate: string
  customerName: string
  settleType: number
  isInvoice: number
  orderStatus: number
  originalRollCount: number
  finishRollCount: number
  paperSummary: string
  processSummary: string
  originalWeight: number
  finishWeight: number
  lossWeight: number
  lossRatio: number
  knifeCount: number
  sawAmount: number
  rewindAmount: number
  processAmount: number
  extraAmount: number
  totalAmount: number
  settledAmount: number
  pendingSettleAmount: number
  receivedAmount: number
  cashReceivedAmount: number
  scrapOffsetAmount: number
  unreceivedAmount: number
}

import {
  getReportDetails,
  getReportQueryMetadata,
  getReportPageAnalysis,
  getReportDimensionAnalysis,
  getReportDimensions,
  getReportMetricContext,
  getReportMetricRelease,
  getReportMetricReleases,
  getReportOverview,
  getReportPaperCandidates,
  getReportProductionAnalysis,
  getReportQualityLossAnalysis,
  getReportCollectionAnalysis,
  getReportDeliveryAnalysis,
  getReportInventoryAnalysis,
  getReportSettlementAnalysis,
} from '../../../api/report'
import { createReportExportTask } from '../../../api/exportTask'
import { pageCustomers } from '../../../api/customer'
import { pageMachines } from '../../../api/machine'
import { pagePapers } from '../../../api/paper'
import type { ReportDetailQuery, ReportExportRequest, ReportQuery } from '../../../types/report'
import type { ReportOperationalAnalysisVO, ReportOperationalTopicCode } from '../../../types/reportOperational'

export type ReportTopicCode = 'production' | 'quality-loss'

const operationalQueries = {
  settlement: getReportSettlementAnalysis,
  collection: getReportCollectionAnalysis,
  inventory: getReportInventoryAnalysis,
  delivery: getReportDeliveryAnalysis,
} satisfies Record<ReportOperationalTopicCode, (query: ReportQuery) => Promise<ReportOperationalAnalysisVO>>

export const reportService = {
  customerCandidates: (keyword: string) => pageCustomers({ current: 1, size: 50, keyword }),
  details: (query: ReportDetailQuery) => getReportDetails(query),
  dimensions: (query: ReportQuery) => getReportDimensions(query),
  dimensionAnalysis: (query: ReportQuery) => getReportDimensionAnalysis(query),
  export: (input: ReportExportRequest) => createReportExportTask(input),
  machines: () => pageMachines({ current: 1, size: 500 }),
  machineCandidates: (keyword: string) => pageMachines({ current: 1, size: 50, keyword }),
  metricContext: () => getReportMetricContext(),
  metricRelease: (releaseUuid: string) => getReportMetricRelease(releaseUuid),
  metricReleases: () => getReportMetricReleases(),
  overview: (query: ReportQuery) => getReportOverview(query),
  pageAnalysis: (query: ReportDetailQuery) => getReportPageAnalysis(query),
  papers: () => pagePapers({ current: 1, size: 500 }),
  paperCandidates: (keyword: string) => getReportPaperCandidates(keyword),
  queryMetadata: (query: ReportQuery) => getReportQueryMetadata(query),
  topicAnalysis: (topic: ReportTopicCode, query: ReportQuery) => topic === 'production'
    ? getReportProductionAnalysis(query)
    : getReportQualityLossAnalysis(query),
  operationalAnalysis: (topic: ReportOperationalTopicCode, query: ReportQuery) =>
    operationalQueries[topic](query),
}

import request from './request'
import type { PageResult } from '../types/common'
import type {
  ReportDetailQuery,
  ReportDetailVO,
  ReportQuery,
  ReportDimensionVO,
  ReportMetricContextVO,
  ReportMetricReleaseDetailVO,
  ReportMetricReleaseSummaryVO,
  ReportOverviewVO,
  ReportQueryExecutionMetaVO,
  ReportQuerySnapshotVO,
  ReportProductionAnalysisVO,
  ReportQualityLossAnalysisVO,
} from '../types/report'
import type {
  ReportCollectionAnalysisVO,
  ReportDeliveryAnalysisVO,
  ReportInventoryAnalysisVO,
  ReportSettlementAnalysisVO,
} from '../types/reportOperational'

export function getReportOverview(query: ReportQuery) {
  return request<ReportOverviewVO>({
    url: '/api/reports/overview',
    method: 'get',
    params: query,
  })
}

export function getReportMetricContext() {
  return request<ReportMetricContextVO>({
    url: '/api/reports/metric-context',
    method: 'get',
  })
}

export function getReportQueryMetadata(query: ReportQuery): Promise<ReportQueryExecutionMetaVO> {
  return request({ url: '/api/reports/query-metadata', method: 'get', params: query })
}

export function createReportQuerySnapshot(query: ReportQuery): Promise<ReportQuerySnapshotVO> {
  return request({ url: '/api/reports/query-snapshots', method: 'post', data: query })
}

export function getReportMetricReleases(): Promise<ReportMetricReleaseSummaryVO[]> {
  return request({ url: '/api/reports/metric-releases', method: 'get' })
}

export function getReportMetricRelease(releaseUuid: string): Promise<ReportMetricReleaseDetailVO> {
  return request({ url: `/api/reports/metric-releases/${releaseUuid}`, method: 'get' })
}

export function getReportDimensions(query: ReportQuery) {
  return request<ReportDimensionVO[]>({
    url: '/api/reports/dimensions',
    method: 'get',
    params: query,
  })
}

export function getReportDetails(query: ReportDetailQuery) {
  return request<PageResult<ReportDetailVO>>({
    url: '/api/reports/details',
    method: 'get',
    params: query,
  })
}

export function getReportPaperCandidates(keyword: string) {
  return request<string[]>({
    url: '/api/reports/candidates/papers',
    method: 'get',
    params: { keyword },
  })
}

export function getReportProductionAnalysis(query: ReportQuery): Promise<ReportProductionAnalysisVO> {
  return request({ url: '/api/reports/topics/production/query', method: 'post', data: query })
}

export function getReportQualityLossAnalysis(query: ReportQuery): Promise<ReportQualityLossAnalysisVO> {
  return request({ url: '/api/reports/topics/quality-loss/query', method: 'post', data: query })
}

export function getReportSettlementAnalysis(query: ReportQuery): Promise<ReportSettlementAnalysisVO> {
  return request({ url: '/api/reports/topics/settlement/query', method: 'post', data: query })
}

export function getReportCollectionAnalysis(query: ReportQuery): Promise<ReportCollectionAnalysisVO> {
  return request({ url: '/api/reports/topics/collection/query', method: 'post', data: query })
}

export function getReportInventoryAnalysis(query: ReportQuery): Promise<ReportInventoryAnalysisVO> {
  return request({ url: '/api/reports/topics/inventory/query', method: 'post', data: query })
}

export function getReportDeliveryAnalysis(query: ReportQuery): Promise<ReportDeliveryAnalysisVO> {
  return request({ url: '/api/reports/topics/delivery/query', method: 'post', data: query })
}

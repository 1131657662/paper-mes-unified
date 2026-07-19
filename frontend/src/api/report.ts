import request from './request'
import type {
  ReportQuery,
  ReportDetailsVO,
  ReportDimensionVO,
  ReportOverviewVO,
} from '../types/report'

export function getReportOverview(query: ReportQuery) {
  return request<ReportOverviewVO>({
    url: '/api/reports/overview',
    method: 'get',
    params: query,
  })
}

export function getReportDimensions(query: ReportQuery) {
  return request<ReportDimensionVO[]>({
    url: '/api/reports/dimensions',
    method: 'get',
    params: query,
  })
}

export function getReportDetails(query: ReportQuery) {
  return request<ReportDetailsVO>({
    url: '/api/reports/details',
    method: 'get',
    params: query,
  })
}

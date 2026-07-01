import request, { rawRequest } from './request'
import type {
  ReportQuery,
  MonthlyReportVO,
  CustomerReportVO,
  LossReportVO,
  MachineReportVO,
  ReportDetailVO,
  ReportDimensionVO,
  ReportOverviewVO,
} from '../types/report'
import { downloadFileFromResponse } from '../utils/downloadFile'

export interface ReportExportResult {
  filename: string
  size: number
}

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
  return request<ReportDetailVO[]>({
    url: '/api/reports/details',
    method: 'get',
    params: query,
  })
}

export async function exportReport(query: ReportQuery): Promise<ReportExportResult> {
  const response = await rawRequest.request<Blob, { data: Blob; headers: Record<string, string> }>({
    url: '/api/reports/export',
    method: 'get',
    params: query,
    responseType: 'blob',
  })
  return downloadFileFromResponse(response, `统计报表_${Date.now()}.xlsx`)
}

export function getMonthlyReport(query: ReportQuery) {
  return request<MonthlyReportVO[]>({
    url: '/api/reports/monthly',
    method: 'get',
    params: query,
  })
}

export function getCustomerReport(query: ReportQuery) {
  return request<CustomerReportVO[]>({
    url: '/api/reports/customer',
    method: 'get',
    params: query,
  })
}

export function getLossReport(query: ReportQuery) {
  return request<LossReportVO[]>({
    url: '/api/reports/loss',
    method: 'get',
    params: query,
  })
}

export function getMachineReport(query: ReportQuery) {
  return request<MachineReportVO[]>({
    url: '/api/reports/machine',
    method: 'get',
    params: query,
  })
}

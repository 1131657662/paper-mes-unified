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
  const filename = filenameFromDisposition(response.headers['content-disposition']) || `统计报表_${Date.now()}.xlsx`
  if (response.data.size <= 0) {
    throw new Error('导出文件为空，请调整筛选条件后重试')
  }
  downloadBlob(response.data, filename)
  return { filename, size: response.data.size }
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

function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

function filenameFromDisposition(disposition?: string) {
  if (!disposition) return undefined
  const match = disposition.match(/filename\*=UTF-8''([^;]+)/)
  return match?.[1] ? decodeURIComponent(match[1]) : undefined
}

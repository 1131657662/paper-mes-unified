import request, { rawRequest } from './request'
import type {
  ReportQuery,
  ReportDetailVO,
  ReportDimensionVO,
  ReportDimension,
  ReportOverviewVO,
} from '../types/report'
import { downloadFileFromResponse } from '../utils/downloadFile'
import { readableExportFilename } from '../utils/documentExport'

const REPORT_DIMENSION_NAME: Record<ReportDimension, string> = {
  month: '月份',
  customer: '客户',
  paper: '纸张',
  process: '工艺',
  machine: '机台',
  invoice: '开票',
  settleType: '结算',
  status: '状态',
}

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
  return downloadFileFromResponse(response, readableExportFilename('统计报表', reportFilenameSuffix(query)))
}

function reportFilenameSuffix(query: ReportQuery) {
  const dimension = query.dimension ? REPORT_DIMENSION_NAME[query.dimension] : '汇总'
  return `${dimension}_${reportPeriod(query)}`
}

function reportPeriod(query: ReportQuery) {
  const from = compactDate(query.dateFrom)
  const to = compactDate(query.dateTo)
  if (from && to) return `${from}-${to}`
  if (from) return `${from}起`
  if (to) return `截至${to}`
  return compactDate(new Date().toISOString()) ?? '当前'
}

function compactDate(value?: string) {
  return value?.slice(0, 10).replaceAll('-', '')
}

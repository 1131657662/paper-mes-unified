import request from './request'
import type {
  ReportQuery,
  MonthlyReportVO,
  CustomerReportVO,
  LossReportVO,
  MachineReportVO,
} from '../types/report'

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

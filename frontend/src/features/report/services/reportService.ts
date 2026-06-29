import {
  getCustomerReport,
  getLossReport,
  getMachineReport,
  getMonthlyReport,
} from '../../../api/report'
import type { ReportQuery } from '../../../types/report'

export const reportService = {
  customer: (query: ReportQuery) => getCustomerReport(query),
  loss: (query: ReportQuery) => getLossReport(query),
  machine: (query: ReportQuery) => getMachineReport(query),
  monthly: (query: ReportQuery) => getMonthlyReport(query),
}

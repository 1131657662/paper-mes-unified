import {
  getCustomerReport,
  getReportDetails,
  getReportDimensions,
  getReportOverview,
  getLossReport,
  getMachineReport,
  getMonthlyReport,
  exportReport,
} from '../../../api/report'
import { pageMachines } from '../../../api/machine'
import { pagePapers } from '../../../api/paper'
import type { ReportQuery } from '../../../types/report'

export const reportService = {
  customer: (query: ReportQuery) => getCustomerReport(query),
  details: (query: ReportQuery) => getReportDetails(query),
  dimensions: (query: ReportQuery) => getReportDimensions(query),
  export: (query: ReportQuery) => exportReport(query),
  loss: (query: ReportQuery) => getLossReport(query),
  machine: (query: ReportQuery) => getMachineReport(query),
  machines: () => pageMachines({ current: 1, size: 500 }),
  monthly: (query: ReportQuery) => getMonthlyReport(query),
  overview: (query: ReportQuery) => getReportOverview(query),
  papers: () => pagePapers({ current: 1, size: 500 }),
}

import {
  getReportDetails,
  getReportDimensions,
  getReportOverview,
  exportReport,
} from '../../../api/report'
import { pageMachines } from '../../../api/machine'
import { pagePapers } from '../../../api/paper'
import type { ReportQuery } from '../../../types/report'

export const reportService = {
  details: (query: ReportQuery) => getReportDetails(query),
  dimensions: (query: ReportQuery) => getReportDimensions(query),
  export: (query: ReportQuery) => exportReport(query),
  machines: () => pageMachines({ current: 1, size: 500 }),
  overview: (query: ReportQuery) => getReportOverview(query),
  papers: () => pagePapers({ current: 1, size: 500 }),
}

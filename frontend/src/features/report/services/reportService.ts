import {
  getReportDetails,
  getReportDimensions,
  getReportOverview,
} from '../../../api/report'
import { createReportExportTask } from '../../../api/exportTask'
import { pageMachines } from '../../../api/machine'
import { pagePapers } from '../../../api/paper'
import type { ReportQuery } from '../../../types/report'

export const reportService = {
  details: (query: ReportQuery) => getReportDetails(query),
  dimensions: (query: ReportQuery) => getReportDimensions(query),
  export: (query: ReportQuery) => createReportExportTask(query),
  machines: () => pageMachines({ current: 1, size: 500 }),
  overview: (query: ReportQuery) => getReportOverview(query),
  papers: () => pagePapers({ current: 1, size: 500 }),
}

import { createReportSavedView, deleteReportSavedView, getReportSavedViews, updateReportSavedView } from '../../../api/reportSavedView'
import type { ReportSavedViewDeleteInput, ReportSavedViewSaveInput, ReportSavedViewUpdateInput } from '../types'

export const reportSavedViewService = {
  list: () => getReportSavedViews(),
  create: (data: ReportSavedViewSaveInput) => createReportSavedView(data),
  update: (input: ReportSavedViewUpdateInput) => updateReportSavedView(input),
  delete: (input: ReportSavedViewDeleteInput) => deleteReportSavedView(input),
}

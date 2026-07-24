import request from './request'
import type { ReportSavedView, ReportSavedViewDeleteInput, ReportSavedViewSaveInput, ReportSavedViewUpdateInput } from '../features/reportSavedView/types'

export function getReportSavedViews(): Promise<ReportSavedView[]> {
  return request({ url: '/api/report-saved-views', method: 'get' })
}

export function createReportSavedView(data: ReportSavedViewSaveInput): Promise<string> {
  return request({ url: '/api/report-saved-views', method: 'post', data })
}

export function updateReportSavedView(input: ReportSavedViewUpdateInput): Promise<void> {
  return request({ url: `/api/report-saved-views/${input.uuid}`, method: 'put', data: input.data })
}

export function deleteReportSavedView(input: ReportSavedViewDeleteInput): Promise<void> {
  return request({ url: `/api/report-saved-views/${input.uuid}`, method: 'delete', params: { version: input.version } })
}

import type { ReportDimension, ReportQuery } from '../../types/report'

export interface ReportSavedView {
  uuid: string
  viewName: string
  reportPath: string
  reportQuery: ReportQuery
  dimensionCode?: ReportDimension
  metricCodes: string[]
  isDefault: 0 | 1
  createTime: string
  updateTime: string
  version: number
}

export interface ReportSavedViewSaveInput {
  viewName: string
  reportPath: string
  reportQuery: ReportQuery
  dimensionCode?: ReportDimension
  metricCodes: string[]
  isDefault: 0 | 1
  version?: number
}

export interface ReportSavedViewUpdateInput { uuid: string; data: ReportSavedViewSaveInput }
export interface ReportSavedViewDeleteInput { uuid: string; version: number }

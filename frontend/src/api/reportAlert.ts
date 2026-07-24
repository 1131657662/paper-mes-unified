import request from './request'
import type {
  ReportAlertRule,
  ReportAlertEventPage,
  ReportAlertEventQuery,
  ReportAlertIgnoreInput,
  ReportAlertRuleDeleteInput,
  ReportAlertRuleSaveInput,
  ReportAlertRuleUpdateInput,
  ReportThresholdContext,
} from '../features/reportAlert/types'
import type { ReportQuery } from '../types/report'

export function getReportThresholdContext(query: ReportQuery): Promise<ReportThresholdContext> {
  return request({
    url: '/api/report-alerts/threshold-context',
    method: 'get',
    params: query,
  })
}

export function getReportAlertRules(): Promise<ReportAlertRule[]> {
  return request({ url: '/api/report-alert-rules', method: 'get' })
}

export function createReportAlertRule(data: ReportAlertRuleSaveInput): Promise<string> {
  return request({ url: '/api/report-alert-rules', method: 'post', data })
}

export function updateReportAlertRule(input: ReportAlertRuleUpdateInput): Promise<void> {
  return request({ url: `/api/report-alert-rules/${input.uuid}`, method: 'put', data: input.data })
}

export function deleteReportAlertRule(input: ReportAlertRuleDeleteInput): Promise<void> {
  return request({
    url: `/api/report-alert-rules/${input.uuid}`,
    method: 'delete',
    params: { version: input.version },
  })
}

export function getReportAlertEvents(query: ReportAlertEventQuery): Promise<ReportAlertEventPage> {
  return request({ url: '/api/report-alert-events', method: 'get', params: query })
}

export function acknowledgeReportAlertEvent(uuid: string): Promise<void> {
  return request({ url: `/api/report-alert-events/${uuid}/acknowledge`, method: 'post' })
}

export function ignoreReportAlertEvent(input: ReportAlertIgnoreInput): Promise<void> {
  return request({ url: `/api/report-alert-events/${input.uuid}/ignore`, method: 'post', data: { reason: input.reason } })
}

import request from './request'
import type {
  ReportSubscription,
  ReportSubscriptionDeleteInput,
  ReportSubscriptionRecipient,
  ReportSubscriptionRunPage,
  ReportSubscriptionRunQuery,
  ReportSubscriptionRunRetryInput,
  ReportSubscriptionSaveInput,
  ReportSubscriptionUpdateInput,
} from '../features/reportSubscription/types'

export function getReportSubscriptions(): Promise<ReportSubscription[]> {
  return request({ url: '/api/report-subscriptions', method: 'get' })
}

export function getReportSubscriptionRecipients(): Promise<ReportSubscriptionRecipient[]> {
  return request({ url: '/api/report-subscriptions/recipient-candidates', method: 'get' })
}

export function createReportSubscription(data: ReportSubscriptionSaveInput): Promise<string> {
  return request({ url: '/api/report-subscriptions', method: 'post', data })
}

export function updateReportSubscription(input: ReportSubscriptionUpdateInput): Promise<void> {
  return request({ url: `/api/report-subscriptions/${input.uuid}`, method: 'put', data: input.data })
}

export function deleteReportSubscription(input: ReportSubscriptionDeleteInput): Promise<void> {
  return request({
    url: `/api/report-subscriptions/${input.uuid}`,
    method: 'delete',
    params: { version: input.version },
  })
}

export function getReportSubscriptionRuns(
  uuid: string,
  query: ReportSubscriptionRunQuery,
): Promise<ReportSubscriptionRunPage> {
  return request({ url: `/api/report-subscriptions/${uuid}/runs`, method: 'get', params: query })
}

export function runReportSubscriptionNow(uuid: string): Promise<string> {
  return request({ url: `/api/report-subscriptions/${uuid}/run-now`, method: 'post' })
}

export function retryReportSubscriptionRun(input: ReportSubscriptionRunRetryInput): Promise<string> {
  return request({
    url: `/api/report-subscriptions/${input.subscriptionUuid}/runs/${input.runUuid}/retry`,
    method: 'post',
  })
}

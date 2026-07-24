import type { ReportQuery, ReportSourcePath } from '../../types/report'

export type ReportScheduleType = 1 | 2 | 3
export type ReportPeriodPolicy = 1 | 2 | 3 | 4
export type ReportSubscriptionRunStatus = 1 | 2 | 3 | 4

export interface ReportSubscriptionRecipient {
  uuid: string
  username: string
  displayName: string
}

export interface ReportSubscription {
  uuid: string
  subscriptionName: string
  reportPath: ReportSourcePath
  scheduleType: ReportScheduleType
  executionTime: string
  weekDay?: number
  monthDay?: number
  timezone: string
  reportQuery: ReportQuery
  periodPolicy: ReportPeriodPolicy
  releasePolicy: 1 | 2
  pinnedReleaseUuid?: string
  isEnabled: 0 | 1
  nextRunAt: string
  lastScheduledAt?: string
  lastErrorMessage?: string
  version: number
  recipients: ReportSubscriptionRecipient[]
}

export interface ReportSubscriptionSaveInput {
  subscriptionName: string
  reportPath: ReportSourcePath
  scheduleType: ReportScheduleType
  executionTime: string
  weekDay?: number
  monthDay?: number
  timezone: string
  reportQuery: ReportQuery
  periodPolicy: ReportPeriodPolicy
  releasePolicy: 1 | 2
  pinnedReleaseUuid?: string
  isEnabled: 0 | 1
  recipientUuids: string[]
  version?: number
}

export interface ReportSubscriptionUpdateInput {
  uuid: string
  data: ReportSubscriptionSaveInput
}

export interface ReportSubscriptionDeleteInput {
  uuid: string
  version: number
}

export interface ReportSubscriptionRun {
  uuid: string
  scheduledFor: string
  metricReleaseUuid: string
  runStatus: ReportSubscriptionRunStatus
  plannedCount: number
  dispatchedCount: number
  failedCount: number
  errorMessage?: string
  completedAt?: string
}

export interface ReportSubscriptionRunQuery {
  current: number
  size: number
  runStatus?: ReportSubscriptionRunStatus
}

export interface ReportSubscriptionRunRetryInput {
  subscriptionUuid: string
  runUuid: string
}

export interface ReportSubscriptionRunPage {
  records: ReportSubscriptionRun[]
  total: number
  current: number
  size: number
}

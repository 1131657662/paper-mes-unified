export type ReportAlertOperator = 'GT' | 'GTE' | 'LT' | 'LTE'
export type ReportAlertScopeType = 1 | 2 | 3 | 4
export type ReportAlertSignalCode = 'LOSS_RATIO' | 'UNRECEIVED_RATIO'
export type ReportAlertEventStatus = 1 | 2 | 3

export interface ReportThresholdItem {
  ruleUuid: string
  signalCode: ReportAlertSignalCode
  comparisonOperator: ReportAlertOperator
  thresholdValue: number
  severity: 1 | 2
  scopeType: ReportAlertScopeType
  scopeLabel: string
}

export interface ReportThresholdContext {
  asOf: string
  thresholds: ReportThresholdItem[]
}

export interface ReportAlertRule {
  uuid: string
  signalCode: ReportAlertSignalCode
  ruleName: string
  scopeType: ReportAlertScopeType
  customerUuid?: string
  paperUuid?: string
  processType?: 1 | 2
  comparisonOperator: ReportAlertOperator
  thresholdValue: number
  severity: 1 | 2
  isEnabled: 0 | 1
  version: number
  updateTime: string
}

export interface ReportAlertRuleSaveInput {
  signalCode: ReportAlertSignalCode
  ruleName: string
  scopeType: ReportAlertScopeType
  customerUuid?: string
  paperUuid?: string
  processType?: 1 | 2
  comparisonOperator: ReportAlertOperator
  thresholdValue: number
  severity: 1 | 2
  isEnabled: 0 | 1
  version?: number
}

export interface ReportAlertRuleUpdateInput {
  uuid: string
  data: ReportAlertRuleSaveInput
}

export interface ReportAlertRuleDeleteInput {
  uuid: string
  version: number
}

export interface ReportAlertEvent {
  uuid: string
  ruleName: string
  signalCode: ReportAlertSignalCode
  scopeLabel: string
  metricReleaseUuid: string
  comparisonOperator: ReportAlertOperator
  periodStart: string
  periodEnd: string
  metricValue: number
  thresholdValue: number
  severity: 1 | 2
  eventStatus: ReportAlertEventStatus
  occurrenceCount: number
  firstDetectedAt: string
  lastDetectedAt: string
  resolvedAt?: string
  acknowledgedAt?: string
  acknowledgedBy?: string
  ignoredAt?: string
  ignoredBy?: string
  ignoreReason?: string
}

export interface ReportAlertEventPage {
  items: ReportAlertEvent[]
  total: number
  page: number
  size: number
  activeCount: number
  resolvedCount: number
  ignoredCount: number
}

export interface ReportAlertEventQuery {
  page: number
  size: number
  status?: ReportAlertEventStatus
  severity?: 1 | 2
  keyword?: string
  focusUuid?: string
}

export interface ReportAlertIgnoreInput {
  uuid: string
  reason: string
}

export type DataHealthSeverity = 'CRITICAL' | 'WARNING'
export type DataHealthRepairAction = 'RECONCILE_SETTLEMENT' | 'RESTORE_COMPLETED_ORDER'

export interface DataHealthIssue {
  issueType: string
  severity: DataHealthSeverity
  businessType: string
  businessUuid: string
  businessNo?: string
  title: string
  detail: string
  repairAction?: DataHealthRepairAction
}

export interface DataHealthSummary {
  checkedAt: string
  criticalCount: number
  warningCount: number
  issues: DataHealthIssue[]
}

export interface DataHealthRepairRequest {
  reason: string
  confirmation: string
}

export interface DataHealthRepairResult {
  businessNo: string
  message: string
}

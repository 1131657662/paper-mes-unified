import type {
  BackRecordCompleteDTO,
  ProcessOrderDetailVO,
} from '../../../types/processOrder'
import type {
  BackRecordAuthorization,
  BackRecordVarianceConfirmation,
} from './backRecordUtils'

interface CompletionSelection {
  completeOrder: boolean
  remainingCount: number
  selectedCount: number
}

export function isCompletionOnly(selection: CompletionSelection): boolean {
  return (
    selection.completeOrder &&
    selection.remainingCount === 0 &&
    selection.selectedCount === 0
  )
}

export function buildBackRecordCompleteDTO(
  detail: ProcessOrderDetailVO,
  authorization?: BackRecordAuthorization,
  variance?: BackRecordVarianceConfirmation,
): BackRecordCompleteDTO {
  return {
    expectedVersion: detail.order.version ?? 0,
    releaseAdminUsername: authorization?.releaseAdminUsername,
    releaseAdminPassword: authorization?.releaseAdminPassword,
    releaseReason: authorization?.releaseReason,
    varianceReason: variance?.varianceReason,
  }
}

export function backRecordSelectionError(
  selection: CompletionSelection,
): string | undefined {
  if (!selection.selectedCount && !isCompletionOnly(selection)) {
    return '请至少选择一个未回录母卷组'
  }
  if (
    selection.completeOrder &&
    selection.selectedCount < selection.remainingCount
  ) {
    return '完成整单前需选中全部未回录母卷组'
  }
  return undefined
}

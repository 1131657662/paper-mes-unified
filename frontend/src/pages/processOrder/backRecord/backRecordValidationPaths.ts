import type { NamePath } from 'antd/es/form/interface'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  activeFinishRolls,
  stepsInBackRecordSelection,
  type BackRecordFormValues,
} from './backRecordUtils'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'

interface ValidationScope {
  completeOrder: boolean
  detail: ProcessOrderDetailVO
  selectedFinishUuids: Set<string>
  selectedItemKeys: Set<string>
  selectedRollUuids: Set<string>
}

export function buildBackRecordValidationPaths(scope: ValidationScope): NamePath[] {
  const paths: NamePath[] = [['warehouseUuid']]
  for (const rollUuid of scope.selectedRollUuids) paths.push(['rolls', rollUuid])

  const items = buildBackRecordWorkbench(scope.detail).items
    .filter((item) => scope.selectedItemKeys.has(item.key))
  for (const item of items) {
    paths.push(['trims', item.key], ['onSiteOutputs', item.key])
  }
  for (const step of stepsInBackRecordSelection(scope.detail, scope.selectedRollUuids)) {
    paths.push(['steps', step.uuid])
  }
  const finishUuids = scope.completeOrder
    ? activeFinishRolls(scope.detail).map((finish) => finish.uuid)
    : scope.selectedFinishUuids
  for (const finishUuid of finishUuids) paths.push(['finishes', finishUuid])
  return paths
}

export function selectedFinishUuidsForSubmission(
  detail: ProcessOrderDetailVO,
  values: BackRecordFormValues,
  selectedFinishUuids: Set<string>,
  selectedRollUuids: Set<string>,
) {
  const result = new Set(selectedFinishUuids)
  for (const finish of activeFinishRolls(detail)) {
    const source = values.finishes?.[finish.uuid]?.originalUuid
    if (source && selectedRollUuids.has(source)) result.add(finish.uuid)
  }
  return result
}

import type { FormInstance } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  buildBackRecordDTO,
  type BackRecordAuthorization,
  type BackRecordFormValues,
  type BackRecordVarianceConfirmation,
} from './backRecordUtils'
import type { useBackRecordSelection } from './useBackRecordSelection'
import {
  buildBackRecordValidationPaths,
  selectedFinishUuidsForSubmission,
} from './backRecordValidationPaths'

interface Options {
  authorization?: BackRecordAuthorization
  completeOrder: boolean
  detail: ProcessOrderDetailVO
  form: FormInstance<BackRecordFormValues>
  selection: ReturnType<typeof useBackRecordSelection>
  variance?: BackRecordVarianceConfirmation
}

export async function prepareBackRecordPayload(options: Options) {
  const formValues = options.form.getFieldsValue(true) as BackRecordFormValues
  const selectedFinishUuids = selectedFinishUuidsForSubmission(
    options.detail, formValues, options.selection.selectedFinishUuids,
    options.selection.selectedRollUuids,
  )
  await options.form.validateFields(buildBackRecordValidationPaths({
    completeOrder: options.completeOrder,
    detail: options.detail,
    selectedFinishUuids,
    selectedItemKeys: options.selection.selectedItemKeys,
    selectedRollUuids: options.selection.selectedRollUuids,
    addedFinishUuids: selectedAddedFinishUuids(formValues, options.selection.selectedItemKeys),
  }), { recursive: true })
  return buildBackRecordDTO(options.detail, formValues, options.authorization, options.variance, {
    completeOrder: options.completeOrder,
    selectedFinishUuids,
    selectedItemKeys: options.selection.selectedItemKeys,
    selectedRollUuids: options.selection.selectedRollUuids,
  })
}

function selectedAddedFinishUuids(values: BackRecordFormValues, selectedItemKeys: Set<string>) {
  return new Set(Object.entries(values.finishAdjustments ?? {})
    .filter(([key]) => selectedItemKeys.has(key))
    .flatMap(([, adjustment]) => adjustment.added.map((added) => added.uuid)))
}

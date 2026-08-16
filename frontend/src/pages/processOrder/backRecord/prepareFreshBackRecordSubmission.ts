import { message, type FormInstance } from 'antd'
import type {
  BackRecordCompleteDTO,
  BackRecordDTO,
  ProcessOrderDetailVO,
} from '../../../types/processOrder'
import type {
  BackRecordAuthorization,
  BackRecordFormValues,
  BackRecordVarianceConfirmation,
} from './backRecordUtils'
import {
  backRecordSelectionError,
  buildBackRecordCompleteDTO,
  isCompletionOnly,
} from './backRecordCompletion'
import { confirmBackRecordIfRequired } from './confirmBackRecordSubmission'
import { prepareBackRecordPayload } from './prepareBackRecordPayload'
import type { useBackRecordSelection } from './useBackRecordSelection'

export interface FreshBackRecordSubmission {
  authorization?: BackRecordAuthorization
  completeOrder: boolean
  detail: ProcessOrderDetailVO
  variance?: BackRecordVarianceConfirmation
}

type PreparedSubmission =
  | { kind: 'back-record'; payload: BackRecordDTO }
  | { kind: 'complete'; payload: BackRecordCompleteDTO }

interface Options {
  form: FormInstance<BackRecordFormValues>
  selectedWarehouseName?: string
  selection: ReturnType<typeof useBackRecordSelection>
  submission: FreshBackRecordSubmission
}

export async function prepareFreshBackRecordSubmission(
  options: Options,
): Promise<PreparedSubmission | undefined> {
  const selection = {
    completeOrder: options.submission.completeOrder,
    remainingCount: options.selection.remainingCount,
    selectedCount: options.selection.selectedCount,
  }
  const selectionError = backRecordSelectionError(selection)
  if (selectionError) {
    message.warning(selectionError)
    return undefined
  }
  if (isCompletionOnly(selection)) return prepareCompletion(options)
  return prepareBatch(options)
}

async function prepareCompletion(
  options: Options,
): Promise<PreparedSubmission | undefined> {
  const payload = buildBackRecordCompleteDTO(
    options.submission.detail,
    options.submission.authorization,
    options.submission.variance,
  )
  const confirmed = await confirm(
    options,
    options.selectedWarehouseName ?? '订单已确定仓库',
  )
  return confirmed ? { kind: 'complete', payload } : undefined
}

async function prepareBatch(
  options: Options,
): Promise<PreparedSubmission | undefined> {
  const payload = await prepareBackRecordPayload({
    ...options.submission,
    form: options.form,
    selection: options.selection,
  })
  const confirmed = await confirm(
    options,
    options.selectedWarehouseName ?? payload.warehouseUuid,
  )
  return confirmed ? { kind: 'back-record', payload } : undefined
}

function confirm(options: Options, warehouseName: string): Promise<boolean> {
  return confirmBackRecordIfRequired({
    orderNo: options.submission.detail.order.orderNo,
    completeOrder: options.submission.completeOrder,
    selectedCount: options.selection.selectedCount,
    skipConfirmation: Boolean(
      options.submission.authorization || options.submission.variance,
    ),
    warehouseName,
  })
}

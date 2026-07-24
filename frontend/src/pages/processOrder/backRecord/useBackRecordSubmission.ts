import { useEffect, useRef, useState } from 'react'
import { Form, message, type FormInstance } from 'antd'
import { BizError, notifyErrorOnce } from '../../../api/request'
import { useBackRecordProcessOrder } from '../../../features/processOrderDetail/hooks/useBackRecordProcessOrder'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import {
  buildBackRecordDTO,
  type BackRecordAuthorization,
  type BackRecordFormValues,
  type BackRecordVarianceConfirmation,
} from './backRecordUtils'
import { confirmBackRecordSubmission } from './confirmBackRecordSubmission'
import { reloadBackRecordConflict } from './reloadBackRecordConflict'
import { showBackRecordResult } from './backRecordResultModal'
import type { useBackRecordSelection } from './useBackRecordSelection'
import {
  buildBackRecordValidationPaths,
  selectedFinishUuidsForSubmission,
} from './backRecordValidationPaths'

interface UseBackRecordSubmissionOptions {
  detail?: ProcessOrderDetailVO
  enabled: boolean
  form: FormInstance<BackRecordFormValues>
  onClose: () => void
  onPersisted?: () => void
  onRefetch: () => Promise<{ data?: ProcessOrderDetailVO; error?: unknown; isSuccess: boolean }>
  onReloaded: (detail: ProcessOrderDetailVO) => void
  onResetInitialization: () => void
  onSuccess: () => void
  selectedWarehouseName?: string
  selection: ReturnType<typeof useBackRecordSelection>
  uuid?: string | null
}

export function useBackRecordSubmission(options: UseBackRecordSubmissionOptions) {
  const [authForm] = Form.useForm<BackRecordAuthorization>()
  const [varianceForm] = Form.useForm<BackRecordVarianceConfirmation>()
  const [authOpen, setAuthOpen] = useState(false)
  const [varianceOpen, setVarianceOpen] = useState(false)
  const completeIntentRef = useRef(true)
  const backRecordMutation = useBackRecordProcessOrder(options.uuid ?? undefined)

  useEffect(() => {
    if (options.enabled) return
    authForm.resetFields()
    varianceForm.resetFields()
    setAuthOpen(false)
    setVarianceOpen(false)
  }, [authForm, options.enabled, varianceForm])

  const submit = async (
    completeOrder = completeIntentRef.current,
    authorization?: BackRecordAuthorization,
    variance?: BackRecordVarianceConfirmation,
  ) => {
    if (!options.detail || !validateSelection(options.selection, completeOrder)) return
    completeIntentRef.current = completeOrder
    const payload = await preparePayload({
      authorization,
      completeOrder,
      detail: options.detail,
      form: options.form,
      selection: options.selection,
      variance,
    })
    if (!authorization && !variance) {
      const confirmed = await confirmBackRecordSubmission({
        orderNo: options.detail.order.orderNo,
        completeOrder,
        selectedCount: options.selection.selectedCount,
        warehouseName: options.selectedWarehouseName ?? payload.warehouseUuid,
      })
      if (!confirmed) return
    }
    await submitPayload(payload)
  }

  const submitPayload = async (payload: ReturnType<typeof buildBackRecordDTO>) => {
    try {
      const result = await backRecordMutation.mutateAsync(payload)
      message.success(result.orderCompleted ? '回录成功，单据已完成' : '本批回录已保存并入库')
      showBackRecordResult(result)
      if (result.orderCompleted) {
        options.onPersisted?.()
        options.onSuccess()
        options.onClose()
        return
      }
      const reload = await reloadBackRecordConflict(options)
      if (!reload.reloaded) {
        notifyErrorOnce(reload.error, '鍥炲綍宸蹭繚瀛橈紝浣嗘湇鍔″櫒鏈€鏂版暟鎹姞杞藉け璐ワ紝璇蜂繚鐣欏綋鍓嶉〉闈㈠苟閲嶈瘯')
      }
    } catch (error) {
      await handleSubmitError(error)
    }
  }

  const handleSubmitError = async (error: unknown) => {
    if (error instanceof BizError && error.errorCode === 'E005') return setAuthOpen(true)
    if (error instanceof BizError && error.errorCode === 'E007') return setVarianceOpen(true)
    if (error instanceof BizError && error.errorCode === 'E006') {
      const reload = await reloadBackRecordConflict(options)
      if (!reload.reloaded) {
        notifyErrorOnce(reload.error, '数据已被他人修改，但服务端最新内容加载失败，请保留当前页面并重试')
        return
      }
      message.warning('数据已被他人修改，已载入服务端最新内容，请重新核对后提交')
      return
    }
    notifyErrorOnce(error, '回录失败，请检查数据后重试')
  }

  const submitAuthorization = async () => {
    const authorization = await authForm.validateFields()
    setAuthOpen(false)
    await submit(completeIntentRef.current, authorization)
  }

  const submitVariance = async () => {
    const variance = await varianceForm.validateFields()
    setVarianceOpen(false)
    await submit(completeIntentRef.current, undefined, variance)
  }

  return {
    authForm,
    authOpen,
    isSubmitting: backRecordMutation.isPending,
    setAuthOpen,
    setVarianceOpen,
    submit,
    submitAuthorization,
    submitVariance,
    varianceForm,
    varianceOpen,
  }
}

function validateSelection(
  selection: ReturnType<typeof useBackRecordSelection>,
  completeOrder: boolean,
) {
  if (!selection.selectedCount) {
    message.warning('请至少选择一个未回录母卷组')
    return false
  }
  if (completeOrder && !selection.allRemainingSelected) {
    message.warning('完成整单前需选中全部未回录母卷组')
    return false
  }
  return true
}

interface PreparePayloadOptions {
  authorization?: BackRecordAuthorization
  completeOrder: boolean
  detail: ProcessOrderDetailVO
  form: FormInstance<BackRecordFormValues>
  selection: ReturnType<typeof useBackRecordSelection>
  variance?: BackRecordVarianceConfirmation
}

async function preparePayload(options: PreparePayloadOptions) {
  const formValues = options.form.getFieldsValue(true) as BackRecordFormValues
  const selectedFinishUuids = selectedFinishUuidsForSubmission(
    options.detail,
    formValues,
    options.selection.selectedFinishUuids,
    options.selection.selectedRollUuids,
  )
  await options.form.validateFields(buildBackRecordValidationPaths({
    completeOrder: options.completeOrder,
    detail: options.detail,
    selectedFinishUuids,
    selectedItemKeys: options.selection.selectedItemKeys,
    selectedRollUuids: options.selection.selectedRollUuids,
  }), { recursive: true })
  return buildBackRecordDTO(options.detail, formValues, options.authorization, options.variance, {
    completeOrder: options.completeOrder,
    selectedFinishUuids,
    selectedItemKeys: options.selection.selectedItemKeys,
    selectedRollUuids: options.selection.selectedRollUuids,
  })
}

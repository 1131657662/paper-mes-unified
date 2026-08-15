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
import { refreshBackRecordBeforeSubmit } from './backRecordSubmissionFreshness'
import { prepareBackRecordPayload } from './prepareBackRecordPayload'
import type { useBackRecordSelection } from './useBackRecordSelection'

interface UseBackRecordSubmissionOptions {
  detail?: ProcessOrderDetailVO
  enabled: boolean
  form: FormInstance<BackRecordFormValues>
  getInitializedVersion: () => number | undefined
  onClose: () => void
  onPersisted?: () => void
  onPersistDraft: () => void
  onRefetch: () => Promise<{ data?: ProcessOrderDetailVO; error?: unknown; isSuccess: boolean }>
  onConflictReloaded: (detail: ProcessOrderDetailVO) => void
  onReloaded: (detail: ProcessOrderDetailVO) => void
  onResetInitialization: () => void
  onSuccess: () => void
  selectedWarehouseName?: string
  selection: ReturnType<typeof useBackRecordSelection>
  uuid?: string | null
}

interface FreshSubmission {
  authorization?: BackRecordAuthorization
  completeOrder: boolean
  detail: ProcessOrderDetailVO
  variance?: BackRecordVarianceConfirmation
}

export function useBackRecordSubmission(options: UseBackRecordSubmissionOptions) {
  const [authForm] = Form.useForm<BackRecordAuthorization>()
  const [varianceForm] = Form.useForm<BackRecordVarianceConfirmation>()
  const [authOpen, setAuthOpen] = useState(false)
  const [varianceOpen, setVarianceOpen] = useState(false)
  const completeIntentRef = useRef(true)
  const submittingRef = useRef(false)
  const [preparing, setPreparing] = useState(false)
  const backRecordMutation = useBackRecordProcessOrder(options.uuid ?? undefined)

  useEffect(() => {
    if (options.enabled) return
    authForm.resetFields()
    varianceForm.resetFields()
    setAuthOpen(false)
    setVarianceOpen(false)
  }, [authForm, options.enabled, varianceForm])

  const refreshSubmissionDetail = async () => {
    if (!options.detail) return undefined
    const refreshed = await refreshBackRecordBeforeSubmit({
      expectedVersion: options.getInitializedVersion() ?? options.detail.order.version,
      onBeforeRefetch: options.onPersistDraft,
      onConflictReloaded: options.onConflictReloaded,
      onRefetch: options.onRefetch,
    })
    if (refreshed.status === 'failed') {
      notifyErrorOnce(refreshed.error, '回录详情刷新失败，请检查网络后重试')
      return undefined
    }
    if (refreshed.status === 'changed') {
      message.warning('加工单已更新，当前填写已保存为本地草稿并完成合并，请核对后再提交')
      return undefined
    }
    return refreshed.detail
  }

  const submitFreshDetail = async (submission: FreshSubmission) => {
    if (!validateSelection(options.selection, submission.completeOrder)) return
    const payload = await prepareBackRecordPayload({
      ...submission, form: options.form, selection: options.selection,
    })
    if (!submission.authorization && !submission.variance) {
      const confirmed = await confirmBackRecordSubmission({
        orderNo: submission.detail.order.orderNo,
        completeOrder: submission.completeOrder,
        selectedCount: options.selection.selectedCount,
        warehouseName: options.selectedWarehouseName ?? payload.warehouseUuid,
      })
      if (!confirmed) return
    }
    await submitPayload(payload)
  }

  const submit = async (
    completeOrder = completeIntentRef.current,
    authorization?: BackRecordAuthorization,
    variance?: BackRecordVarianceConfirmation,
  ) => {
    if (submittingRef.current) return
    if (!options.detail) return
    submittingRef.current = true
    setPreparing(true)
    completeIntentRef.current = completeOrder
    try {
      const detail = await refreshSubmissionDetail()
      if (detail) await submitFreshDetail({ authorization, completeOrder, detail, variance })
    } finally {
      submittingRef.current = false
      setPreparing(false)
    }
  }

  const submitPayload = async (payload: ReturnType<typeof buildBackRecordDTO>) => {
    try {
      options.onPersistDraft()
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
        notifyErrorOnce(reload.error, '回录已保存，但服务器最新数据加载失败，请保留当前页面并重试')
      }
    } catch (error) {
      await handleSubmitError(error)
    }
  }

  const handleSubmitError = async (error: unknown) => {
    if (error instanceof BizError && error.errorCode === 'E005') return setAuthOpen(true)
    if (error instanceof BizError && error.errorCode === 'E007') return setVarianceOpen(true)
    if (error instanceof BizError && error.errorCode === 'E006') {
      const reload = await reloadBackRecordConflict({
        ...options,
        onReloaded: options.onConflictReloaded,
        preserveDirty: true,
      })
      if (!reload.reloaded) {
        notifyErrorOnce(reload.error, '数据已被他人修改，但服务端最新内容加载失败，请保留当前页面并重试')
        return
      }
      message.warning('数据已被他人修改，已合并服务端最新内容并保留本地草稿，请重新核对后提交')
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
    isSubmitting: preparing || backRecordMutation.isPending,
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
  const noPendingRolls = selection.remainingCount === 0
  if (!selection.selectedCount && !(completeOrder && noPendingRolls)) {
    message.warning('请至少选择一个未回录母卷组')
    return false
  }
  if (completeOrder && !selection.allRemainingSelected) {
    message.warning('完成整单前需选中全部未回录母卷组')
    return false
  }
  return true
}

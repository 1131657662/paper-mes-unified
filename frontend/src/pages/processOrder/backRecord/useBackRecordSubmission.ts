import { useEffect, useRef, useState } from 'react'
import { Form, message, type FormInstance } from 'antd'
import { notifyErrorOnce } from '../../../api/request'
import { useBackRecordProcessOrder } from '../../../features/processOrderDetail/hooks/useBackRecordProcessOrder'
import { useCompleteBackRecordOrder } from '../../../features/processOrderDetail/hooks/useCompleteBackRecordOrder'
import type {
  BackRecordResultVO,
  ProcessOrderDetailVO,
} from '../../../types/processOrder'
import {
  type BackRecordAuthorization,
  type BackRecordFormValues,
  type BackRecordVarianceConfirmation,
} from './backRecordUtils'
import { reloadBackRecordConflict } from './reloadBackRecordConflict'
import { showBackRecordResult } from './backRecordResultModal'
import { refreshBackRecordBeforeSubmit } from './backRecordSubmissionFreshness'
import {
  prepareFreshBackRecordSubmission,
  type FreshBackRecordSubmission,
} from './prepareFreshBackRecordSubmission'
import type { useBackRecordSelection } from './useBackRecordSelection'
import { handleBackRecordSubmissionError } from './handleBackRecordSubmissionError'

interface UseBackRecordSubmissionOptions {
  detail?: ProcessOrderDetailVO
  enabled: boolean
  form: FormInstance<BackRecordFormValues>
  getInitializedVersion: () => number | undefined
  onClose: () => void
  onPersisted?: () => void
  onPersistDraft: () => void
  onRefetch: () => Promise<{
    data?: ProcessOrderDetailVO
    error?: unknown
    isSuccess: boolean
  }>
  onConflictReloaded: (detail: ProcessOrderDetailVO) => void
  onReloaded: (detail: ProcessOrderDetailVO) => void
  onResetInitialization: () => void
  onSuccess: () => void
  selectedWarehouseName?: string
  selection: ReturnType<typeof useBackRecordSelection>
  uuid?: string | null
}
export function useBackRecordSubmission(
  options: UseBackRecordSubmissionOptions,
) {
  const [authForm] = Form.useForm<BackRecordAuthorization>()
  const [varianceForm] = Form.useForm<BackRecordVarianceConfirmation>()
  const [authOpen, setAuthOpen] = useState(false)
  const [varianceOpen, setVarianceOpen] = useState(false)
  const completeIntentRef = useRef(true)
  const submittingRef = useRef(false)
  const [preparing, setPreparing] = useState(false)
  const backRecordMutation = useBackRecordProcessOrder(
    options.uuid ?? undefined,
  )
  const completionMutation = useCompleteBackRecordOrder(
    options.uuid ?? undefined,
  )

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
      expectedVersion:
        options.getInitializedVersion() ?? options.detail.order.version,
      onBeforeRefetch: options.onPersistDraft,
      onConflictReloaded: options.onConflictReloaded,
      onRefetch: options.onRefetch,
    })
    if (refreshed.status === 'failed') {
      notifyErrorOnce(refreshed.error, '回录详情刷新失败，请检查网络后重试')
      return undefined
    }
    if (refreshed.status === 'changed') {
      message.warning(
        '加工单已更新，当前填写已保存为本地草稿并完成合并，请核对后再提交',
      )
      return undefined
    }
    return refreshed.detail
  }
  const submitFreshDetail = async (submission: FreshBackRecordSubmission) => {
    const prepared = await prepareFreshBackRecordSubmission({
      form: options.form,
      selectedWarehouseName: options.selectedWarehouseName,
      selection: options.selection,
      submission,
    })
    if (!prepared) return
    if (prepared.kind === 'complete') {
      await submitPayload(() =>
        completionMutation.mutateAsync(prepared.payload),
      )
      return
    }
    await submitPayload(() => backRecordMutation.mutateAsync(prepared.payload))
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
      if (detail)
        await submitFreshDetail({
          authorization,
          completeOrder,
          detail,
          variance,
        })
    } finally {
      submittingRef.current = false
      setPreparing(false)
    }
  }

  const submitPayload = async (
    submitRequest: () => Promise<BackRecordResultVO>,
  ) => {
    try {
      options.onPersistDraft()
      const result = await submitRequest()
      message.success(
        result.orderStatus === 6
          ? '回录已关闭，订单已作废'
          : result.orderCompleted
            ? '回录成功，单据已完成'
            : '本批回录已保存并入库',
      )
      showBackRecordResult(result)
      if (result.orderCompleted) {
        options.onPersisted?.()
        options.onSuccess()
        options.onClose()
        return
      }
      const reload = await reloadBackRecordConflict(options)
      if (!reload.reloaded) {
        notifyErrorOnce(
          reload.error,
          '回录已保存，但服务器最新数据加载失败，请保留当前页面并重试',
        )
      }
    } catch (error) {
      await handleBackRecordSubmissionError({
        error,
        onAuthorizationRequired: () => setAuthOpen(true),
        onVarianceRequired: () => setVarianceOpen(true),
        reloadConflict: () =>
          reloadBackRecordConflict({
            ...options,
            onReloaded: options.onConflictReloaded,
            preserveDirty: true,
          }),
      })
    }
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
    isSubmitting:
      preparing || backRecordMutation.isPending || completionMutation.isPending,
    setAuthOpen,
    setVarianceOpen,
    submit,
    submitAuthorization,
    submitVariance,
    varianceForm,
    varianceOpen,
  }
}

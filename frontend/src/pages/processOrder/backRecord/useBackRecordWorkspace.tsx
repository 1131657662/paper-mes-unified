import { useEffect, useRef, useState } from 'react'
import { Form, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import type { ProcessStepDTO } from '../../../api/processOrder'
import { BizError, notifyErrorOnce } from '../../../api/request'
import { useAddProcessStep } from '../../../features/processOrderDetail/hooks/useAddProcessStep'
import { useBackRecordProcessOrder } from '../../../features/processOrderDetail/hooks/useBackRecordProcessOrder'
import { useChangeOrderStatus } from '../../../features/processOrderDetail/hooks/useChangeOrderStatus'
import { useProcessOrderDetail } from '../../../features/processOrderDetail/hooks/useProcessOrderDetail'
import { useRollbackProcessOrderToDraft } from '../../../features/processOrderDetail/hooks/useRollbackProcessOrderToDraft'
import { confirmOrderStatusChange } from '../../../features/processOrderDetail/confirmOrderStatusChange'
import BackRecordWorkspaceModals from './BackRecordWorkspaceModals'
import {
  buildBackRecordDTO,
  initialBackRecordValues,
  type BackRecordAuthorization,
  type BackRecordFormValues,
  type BackRecordVarianceConfirmation,
} from './backRecordUtils'
import { showBackRecordResult } from './backRecordResultModal'
import { confirmBackRecordSubmission } from './confirmBackRecordSubmission'
import { buildInitialOnSiteOutputGroups } from './backRecordOnSiteOutputModel'
import { buildBackRecordWorkbench } from './backRecordWorkbenchUtils'
import { useBackRecordDisplayValues } from './useBackRecordDisplayValues'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import { useBackRecordWarehouseSelection } from './useBackRecordWarehouseSelection'

interface Params {
  uuid?: string | null
  enabled?: boolean
  onClose: () => void
  onSuccess: () => void
}

export function useBackRecordWorkspace({ uuid, enabled = true, onClose, onSuccess }: Params) {
  const navigate = useNavigate()
  const [form] = Form.useForm<BackRecordFormValues>()
  const [authForm] = Form.useForm<BackRecordAuthorization>()
  const [varianceForm] = Form.useForm<BackRecordVarianceConfirmation>()
  const [authOpen, setAuthOpen] = useState(false)
  const [varianceOpen, setVarianceOpen] = useState(false)
  const [changeOpen, setChangeOpen] = useState(false)
  const [changeItem, setChangeItem] = useState<BackRecordWorkItem | null>(null)
  const [stepFormOpen, setStepFormOpen] = useState(false)
  const detailQuery = useProcessOrderDetail(uuid ?? undefined, { enabled })
  const backRecordMutation = useBackRecordProcessOrder(uuid ?? undefined)
  const addStepMutation = useAddProcessStep()
  const changeStatusMutation = useChangeOrderStatus()
  const rollbackDraftMutation = useRollbackProcessOrderToDraft()
  const [filledValues, setFilledValues] = useState<BackRecordFormValues>({})
  const initializedOrderRef = useRef<string | null>(null)
  const displayValues = useBackRecordDisplayValues(form, filledValues)
  const warehouse = useBackRecordWarehouseSelection({ detail: detailQuery.data, enabled, form })

  useEffect(() => {
    const detailUuid = detailQuery.data?.order.uuid
    if (enabled && detailQuery.data && initializedOrderRef.current !== detailUuid) {
      const initialValues = initialBackRecordValues(detailQuery.data)
      const workbench = buildBackRecordWorkbench(detailQuery.data)
      initialValues.onSiteOutputs = buildInitialOnSiteOutputGroups(detailQuery.data, workbench.items)
      form.setFieldsValue(initialValues)
      setFilledValues(initialValues)
      initializedOrderRef.current = detailUuid ?? null
    }
    if (!enabled) {
      initializedOrderRef.current = null
      form.resetFields()
      setFilledValues({})
      authForm.resetFields()
      varianceForm.resetFields()
      setAuthOpen(false)
      setVarianceOpen(false)
      setChangeOpen(false)
      setChangeItem(null)
      setStepFormOpen(false)
    }
  }, [enabled, detailQuery.data, form, authForm, varianceForm])

  const submit = async (authorization?: BackRecordAuthorization, variance?: BackRecordVarianceConfirmation) => {
    if (!detailQuery.data) return
    await form.validateFields()
    const formValues = form.getFieldsValue(true) as BackRecordFormValues
    const payload = buildBackRecordDTO(detailQuery.data, formValues, authorization, variance)
    if (!authorization && !variance) {
      const confirmed = await confirmBackRecordSubmission({
        orderNo: detailQuery.data.order.orderNo,
        warehouseName: warehouse.selectedName ?? payload.warehouseUuid,
      })
      if (!confirmed) return
    }
    await submitPayload(payload)
  }

  const submitPayload = async (payload: ReturnType<typeof buildBackRecordDTO>) => {
    try {
      const result = await backRecordMutation.mutateAsync(payload)
      message.success('回录成功，单据已完成')
      showBackRecordResult(result)
      onSuccess()
      onClose()
    } catch (error) {
      await handleSubmitError(error)
    }
  }

  const handleSubmitError = async (error: unknown) => {
    const bizError = error as BizError
    if (bizError.errorCode === 'E005') return setAuthOpen(true)
    if (bizError.errorCode === 'E007') return setVarianceOpen(true)
    if (bizError.errorCode === 'E006') {
      await detailQuery.refetch()
      return
    }
    notifyErrorOnce(error, '回录失败，请检查数据后重试')
  }

  const submitAuthorization = async () => {
    const authorization = await authForm.validateFields()
    setAuthOpen(false)
    await submit(authorization)
  }

  const submitVariance = async () => {
    const variance = await varianceForm.validateFields()
    setVarianceOpen(false)
    await submit(undefined, variance)
  }

  const openChangeGuide = (item: BackRecordWorkItem | null) => {
    setChangeItem(item)
    setChangeOpen(true)
  }

  const addExtraStep = async (values: ProcessStepDTO) => {
    if (!uuid) return
    await addStepMutation.mutateAsync({ orderUuid: uuid, values: { ...values, isMain: 0 } })
    message.success('追加工序已记录，计费已重算')
    setStepFormOpen(false)
    setChangeOpen(false)
    initializedOrderRef.current = null
    await detailQuery.refetch()
  }

  const rollbackToConfig = async () => {
    if (!uuid) return
    confirmOrderStatusChange({
      title: '确认回退到待下发重配？',
      orderNo: detailQuery.data?.order.orderNo,
      okText: '确认回退',
      danger: true,
      requireReason: true,
      reasonPlaceholder: '请填写回退原因，例如：现场主方案变更、规格调整',
      onConfirm: async (reason) => {
        await changeStatusMutation.mutateAsync({ orderUuid: uuid, reason, targetStatus: 1 })
        message.success('已回退到待下发，请重新配置工艺后再下发')
        setChangeOpen(false)
        onSuccess()
        onClose()
      },
    })
  }

  const rollbackToDraft = async () => {
    if (!uuid) return
    confirmOrderStatusChange({
      title: '确认回退到草稿更换母卷？',
      orderNo: detailQuery.data?.order.orderNo,
      okText: '确认回退',
      danger: true,
      requireReason: true,
      reasonPlaceholder: '请填写回退原因，例如：母卷更换、规格录错、方案重做',
      onConfirm: async (reason) => {
        await rollbackDraftMutation.mutateAsync({ orderUuid: uuid, reason: reason ?? '' })
        message.success('已回退到草稿，请重新编辑母卷和加工方案')
        setChangeOpen(false)
        onSuccess()
        navigate(`/process-orders/create?draft=${uuid}`)
      },
    })
  }

  return {
    detail: detailQuery.data,
    form,
    isLoadingDetail: detailQuery.isLoading,
    isSubmitting: backRecordMutation.isPending,
    modals: <BackRecordWorkspaceModals
        authForm={authForm}
        authOpen={authOpen}
        varianceForm={varianceForm}
        varianceOpen={varianceOpen}
        changeItem={changeItem}
        changeOpen={changeOpen}
        detail={detailQuery.data ?? null}
        addingStep={addStepMutation.isPending}
        rollingBack={changeStatusMutation.isPending || rollbackDraftMutation.isPending}
        onAddExtraStep={addExtraStep}
        onCancelAuth={() => setAuthOpen(false)}
        onCancelVariance={() => setVarianceOpen(false)}
        onCancelChange={() => setChangeOpen(false)}
        onCancelStep={() => setStepFormOpen(false)}
        onOpenStep={() => setStepFormOpen(true)}
        onRollbackToDraft={rollbackToDraft}
        onRollbackToConfig={rollbackToConfig}
        onSubmitAuth={submitAuthorization}
        onSubmitVariance={submitVariance}
        stepFormOpen={stepFormOpen}
      />,
    values: displayValues,
    warehouse,
    syncFilledValues: setFilledValues,
    openChangeGuide,
    submit,
  }
}

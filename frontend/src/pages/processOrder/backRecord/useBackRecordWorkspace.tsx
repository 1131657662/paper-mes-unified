import { useEffect, useState } from 'react'
import { Form, message } from 'antd'
import type { ProcessStepDTO } from '../../../api/processOrder'
import { BizError } from '../../../api/request'
import { useAddProcessStep } from '../../../features/processOrderDetail/hooks/useAddProcessStep'
import { useBackRecordProcessOrder } from '../../../features/processOrderDetail/hooks/useBackRecordProcessOrder'
import { useChangeOrderStatus } from '../../../features/processOrderDetail/hooks/useChangeOrderStatus'
import { useProcessOrderDetail } from '../../../features/processOrderDetail/hooks/useProcessOrderDetail'
import BackRecordWorkspaceModals from './BackRecordWorkspaceModals'
import {
  buildBackRecordDTO,
  initialBackRecordValues,
  type BackRecordAuthorization,
  type BackRecordFormValues,
} from './backRecordUtils'
import { showBackRecordResult } from './backRecordResultModal'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Params {
  uuid?: string | null
  enabled?: boolean
  onClose: () => void
  onSuccess: () => void
}

export function useBackRecordWorkspace({ uuid, enabled = true, onClose, onSuccess }: Params) {
  const [form] = Form.useForm<BackRecordFormValues>()
  const [authForm] = Form.useForm<BackRecordAuthorization>()
  const [authOpen, setAuthOpen] = useState(false)
  const [changeOpen, setChangeOpen] = useState(false)
  const [changeItem, setChangeItem] = useState<BackRecordWorkItem | null>(null)
  const [stepFormOpen, setStepFormOpen] = useState(false)
  const detailQuery = useProcessOrderDetail(uuid ?? undefined, { enabled })
  const backRecordMutation = useBackRecordProcessOrder(uuid ?? undefined)
  const addStepMutation = useAddProcessStep()
  const changeStatusMutation = useChangeOrderStatus()
  const values = useBackRecordFormValues(form)
  const [filledValues, setFilledValues] = useState<BackRecordFormValues>({})
  const displayValues = mergeBackRecordValues(values, filledValues)

  useEffect(() => {
    if (enabled && detailQuery.data) {
      const initialValues = initialBackRecordValues(detailQuery.data)
      form.setFieldsValue(initialValues)
      setFilledValues(initialValues)
    }
    if (!enabled) {
      form.resetFields()
      setFilledValues({})
      authForm.resetFields()
      setAuthOpen(false)
      setChangeOpen(false)
      setChangeItem(null)
      setStepFormOpen(false)
    }
  }, [enabled, detailQuery.data, form, authForm])

  const submit = async (authorization?: BackRecordAuthorization) => {
    if (!detailQuery.data) return
    await form.validateFields()
    const formValues = form.getFieldsValue(true) as BackRecordFormValues
    const payload = buildBackRecordDTO(detailQuery.data, formValues, authorization)
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
    if (bizError.errorCode === 'E006') {
      message.error('数据已被他人修改，已刷新当前单据')
      await detailQuery.refetch()
      return
    }
    message.error(bizError.message || '回录失败，请检查数据后重试')
  }

  const submitAuthorization = async () => {
    const authorization = await authForm.validateFields()
    setAuthOpen(false)
    await submit(authorization)
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
    await detailQuery.refetch()
  }

  const rollbackToConfig = async () => {
    if (!uuid) return
    await changeStatusMutation.mutateAsync({ orderUuid: uuid, targetStatus: 1 })
    message.success('已回退到待下发，请重新配置工艺后再下发')
    setChangeOpen(false)
    onSuccess()
    onClose()
  }

  return {
    detail: detailQuery.data,
    form,
    isLoadingDetail: detailQuery.isLoading,
    isSubmitting: backRecordMutation.isPending,
    modals: <BackRecordWorkspaceModals
        authForm={authForm}
        authOpen={authOpen}
        changeItem={changeItem}
        changeOpen={changeOpen}
        detail={detailQuery.data ?? null}
        addingStep={addStepMutation.isPending}
        rollingBack={changeStatusMutation.isPending}
        onAddExtraStep={addExtraStep}
        onCancelAuth={() => setAuthOpen(false)}
        onCancelChange={() => setChangeOpen(false)}
        onCancelStep={() => setStepFormOpen(false)}
        onOpenStep={() => setStepFormOpen(true)}
        onRollbackToConfig={rollbackToConfig}
        onSubmitAuth={submitAuthorization}
        stepFormOpen={stepFormOpen}
      />,
    values: displayValues,
    syncFilledValues: setFilledValues,
    openChangeGuide,
    submit,
  }
}

function useBackRecordFormValues(form: ReturnType<typeof Form.useForm<BackRecordFormValues>>[0]) {
  const rolls = Form.useWatch('rolls', form)
  const finishes = Form.useWatch('finishes', form)
  return { rolls, finishes }
}

function mergeBackRecordValues(
  watched: BackRecordFormValues,
  fallback: BackRecordFormValues,
): BackRecordFormValues {
  return {
    ...fallback,
    ...watched,
    finishes: { ...fallback.finishes, ...watched.finishes },
    rolls: { ...fallback.rolls, ...watched.rolls },
  }
}

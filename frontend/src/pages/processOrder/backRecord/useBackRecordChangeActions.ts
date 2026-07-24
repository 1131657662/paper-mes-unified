import { useEffect, useState } from 'react'
import { message } from 'antd'
import { useNavigate } from 'react-router-dom'
import type { ProcessStepDTO } from '../../../api/processOrder'
import { confirmOrderStatusChange } from '../../../features/processOrderDetail/confirmOrderStatusChange'
import { useAddProcessStep } from '../../../features/processOrderDetail/hooks/useAddProcessStep'
import { useChangeOrderStatus } from '../../../features/processOrderDetail/hooks/useChangeOrderStatus'
import { useRollbackProcessOrderToDraft } from '../../../features/processOrderDetail/hooks/useRollbackProcessOrderToDraft'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface UseBackRecordChangeActionsOptions {
  detail?: ProcessOrderDetailVO
  enabled: boolean
  onClose: () => void
  onRefetch: () => Promise<unknown>
  onResetInitialization: () => void
  onSuccess: () => void
  uuid?: string | null
}

export function useBackRecordChangeActions(options: UseBackRecordChangeActionsOptions) {
  const navigate = useNavigate()
  const [changeOpen, setChangeOpen] = useState(false)
  const [changeItem, setChangeItem] = useState<BackRecordWorkItem | null>(null)
  const [stepFormOpen, setStepFormOpen] = useState(false)
  const addStepMutation = useAddProcessStep()
  const changeStatusMutation = useChangeOrderStatus()
  const rollbackDraftMutation = useRollbackProcessOrderToDraft()

  useEffect(() => {
    if (options.enabled) return
    setChangeOpen(false)
    setChangeItem(null)
    setStepFormOpen(false)
  }, [options.enabled])

  const addExtraStep = async (values: ProcessStepDTO) => {
    if (!options.uuid) return
    await addStepMutation.mutateAsync({
      orderUuid: options.uuid,
      values: { ...values, isMain: 0 },
    })
    message.success('追加工序已记录，计费已重算')
    setStepFormOpen(false)
    setChangeOpen(false)
    options.onResetInitialization()
    await options.onRefetch()
  }

  const rollbackToConfig = async () => {
    if (!options.uuid) return
    confirmOrderStatusChange({
      title: '确认回退到待下发重配？',
      orderNo: options.detail?.order.orderNo,
      okText: '确认回退',
      danger: true,
      requireReason: true,
      reasonPlaceholder: '请填写回退原因，例如：现场主方案变更、规格调整',
      onConfirm: async (reason) => {
        await changeStatusMutation.mutateAsync({
          orderUuid: options.uuid!,
          reason,
          targetStatus: 1,
        })
        message.success('已回退到待下发，请重新配置工艺后再下发')
        setChangeOpen(false)
        options.onSuccess()
        options.onClose()
      },
    })
  }

  const rollbackToDraft = async () => {
    if (!options.uuid) return
    confirmOrderStatusChange({
      title: '确认回退到草稿更换母卷？',
      orderNo: options.detail?.order.orderNo,
      okText: '确认回退',
      danger: true,
      requireReason: true,
      reasonPlaceholder: '请填写回退原因，例如：母卷更换、规格录错、方案重做',
      onConfirm: async (reason) => {
        await rollbackDraftMutation.mutateAsync({
          orderUuid: options.uuid!,
          reason: reason ?? '',
        })
        message.success('已回退到草稿，请重新编辑母卷和加工方案')
        setChangeOpen(false)
        options.onSuccess()
        navigate(`/process-orders/create?draft=${options.uuid}`)
      },
    })
  }

  return {
    addingStep: addStepMutation.isPending,
    changeItem,
    changeOpen,
    openChangeGuide: (item: BackRecordWorkItem | null) => {
      setChangeItem(item)
      setChangeOpen(true)
    },
    rollbackToConfig,
    rollbackToDraft,
    rollingBack: changeStatusMutation.isPending || rollbackDraftMutation.isPending,
    setChangeOpen,
    setStepFormOpen,
    stepFormOpen,
    addExtraStep,
  }
}

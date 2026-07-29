import { useEffect, useState } from 'react'
import { message } from 'antd'
import { useNavigate } from 'react-router'
import { notifyErrorOnce } from '../../../api/request'
import type { ProcessStepDTO } from '../../../api/processOrder'
import { confirmOrderStatusChange } from '../../../features/processOrderDetail/confirmOrderStatusChange'
import { useAddProcessStep } from '../../../features/processOrderDetail/hooks/useAddProcessStep'
import { useChangeOrderStatus } from '../../../features/processOrderDetail/hooks/useChangeOrderStatus'
import { useRollbackProcessOrderToDraft } from '../../../features/processOrderDetail/hooks/useRollbackProcessOrderToDraft'
import { useReopenBackRecordBatch } from '../../../features/processOrderDetail/hooks/useReopenBackRecordBatch'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import { workItemRollUuids } from './backRecordWorkbenchUtils'
import { reloadBackRecordConflict } from './reloadBackRecordConflict'

interface UseBackRecordChangeActionsOptions {
  detail?: ProcessOrderDetailVO
  enabled: boolean
  onClose: () => void
  onPersisted?: () => void
  onRefetch: () => Promise<{ data?: ProcessOrderDetailVO; error?: unknown; isSuccess: boolean }>
  onReloaded: (detail: ProcessOrderDetailVO) => void
  onResetInitialization: () => void
  onSelectAfterRefresh: (keys: string[]) => void
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
  const reopenBatchMutation = useReopenBackRecordBatch()

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

  const reopenBatch = async (item: BackRecordWorkItem) => {
    if (!options.uuid || !options.detail) return
    const rollUuids = workItemRollUuids(item)
    await reopenBatchMutation.mutateAsync({
      orderUuid: options.uuid,
      values: {
        expectedVersion: options.detail.order.version ?? 0,
        rollUuids,
      },
    })
    options.onSelectAfterRefresh([item.key])
    const reload = await reloadBackRecordConflict(options)
    if (!reload.reloaded) {
      notifyErrorOnce(reload.error, '回录已撤回，但服务器最新数据加载失败，请保留当前页面并重试')
      return
    }
    message.success('本批回录已撤回，可继续修改后重新保存')
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
    reopenBatch,
    reopening: reopenBatchMutation.isPending,
    rollingBack: changeStatusMutation.isPending || rollbackDraftMutation.isPending,
    setChangeOpen,
    setStepFormOpen,
    stepFormOpen,
    addExtraStep,
  }
}

import { useState } from 'react'
import { Input, Modal, message } from 'antd'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import FinishRollManageDrawer from '../../../pages/processOrder/FinishRollManageDrawer'
import SnapshotDiffModal from '../../../pages/processOrder/SnapshotDiffModal'
import { queries } from '../../../queries'
import { useCalcProcessOrderFee } from '../hooks/useCalcProcessOrderFee'
import { useChangeOrderStatus } from '../hooks/useChangeOrderStatus'
import { useRollbackProcessOrderToDraft } from '../hooks/useRollbackProcessOrderToDraft'
import { useVoidProcessOrder } from '../hooks/useVoidProcessOrder'
import { confirmOrderStatusChange, isRollbackStatusChange } from '../confirmOrderStatusChange'
import OrderExecutionPanel from './OrderExecutionPanel'
import PrintIssueDrawer from './PrintIssueDrawer'

interface Props {
  detail?: ProcessOrderDetailVO
}

export default function OrderExecutionHost({ detail }: Props) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const orderUuid = detail?.order.uuid
  const [printOpen, setPrintOpen] = useState(false)
  const [diffOpen, setDiffOpen] = useState(false)
  const [manageRollOpen, setManageRollOpen] = useState(false)
  const { mutateAsync: changeStatus, isPending: isChangingStatus } = useChangeOrderStatus()
  const { mutateAsync: rollbackToDraft, isPending: isRollingBackDraft } = useRollbackProcessOrderToDraft()
  const { mutateAsync: calcFee, isPending: isCalculatingFee } = useCalcProcessOrderFee(orderUuid)
  const { mutateAsync: voidOrder, isPending: isVoidingOrder } = useVoidProcessOrder()
  const capabilities = {
    canManageOrder: useHasPermission(PERMISSIONS.orderManage),
    canCreateOrder: useHasPermission(PERMISSIONS.orderCreate),
    canBackRecord: useHasPermission(PERMISSIONS.orderBackRecord),
    canManageDelivery: useHasPermission(PERMISSIONS.deliveryManage),
    canManageSettlement: useHasPermission(PERMISSIONS.settleManage),
  }

  if (!detail || !orderUuid) return null

  const refreshDetail = async () => {
    await queryClient.invalidateQueries({
      queryKey: queries.processOrderDetail.detail(orderUuid).queryKey,
    })
  }

  const handleChangeStatus = async (targetStatus: number, reason?: string) => {
    if (targetStatus === 0) {
      await rollbackToDraft({ orderUuid, reason: reason ?? '' })
    } else {
      await changeStatus({ orderUuid, reason, targetStatus })
    }
    message.success('状态已更新')
    if (targetStatus === 0) {
      navigate(`/process-orders/create?draft=${orderUuid}`)
    }
  }

  const handleConfirmStatus = (targetStatus: number, title: string) => {
    const currentStatus = detail.order.orderStatus ?? 0
    const requireReason = isRollbackStatusChange(currentStatus, targetStatus)
    confirmOrderStatusChange({
      title,
      orderNo: detail.order.orderNo,
      okText: requireReason ? '确认回退' : '确认',
      danger: requireReason,
      requireReason,
      reasonPlaceholder: '请填写回退原因，例如：客户改单、现场方案调整、备注补充',
      onConfirm: (reason) => handleChangeStatus(targetStatus, reason),
    })
  }

  const handleCalcFee = async () => {
    const result = await calcFee()
    message.success(`计费已更新，总额 ¥${result.totalAmount ?? 0}`)
  }

  const handleVoidOrder = () => {
    let reason = ''
    Modal.confirm({
      title: '作废加工单',
      content: (
        <Input.TextArea
          autoSize={{ minRows: 3, maxRows: 5 }}
          maxLength={255}
          placeholder="请填写作废原因"
          showCount
          onChange={(event) => {
            reason = event.target.value
          }}
        />
      ),
      okButtonProps: { danger: true },
      okText: '确认作废',
      cancelText: '取消',
      onOk: async () => {
        const trimmed = reason.trim()
        if (!trimmed) {
          message.warning('请填写作废原因')
          throw new Error('作废原因不能为空')
        }
        await voidOrder({ orderUuid, reason: trimmed })
        message.success('加工单已作废')
      },
    })
  }

  return (
    <>
      <OrderExecutionPanel
        detail={detail}
        capabilities={capabilities}
        actions={{
          onPrint: () => setPrintOpen(true),
          onBackRecord: () => navigate(`/process-orders/${orderUuid}/back-record`),
          onSnapshotDiff: () => setDiffOpen(true),
          onManageRolls: () => setManageRollOpen(true),
          onEditDraft: () => navigate(`/process-orders/create?draft=${orderUuid}`),
          onChangeStatus: handleConfirmStatus,
          onCalcFee: handleCalcFee,
          onGoDelivery: () => navigate('/delivery-orders'),
          onGoSettle: () => navigate('/settle-orders'),
          onVoidOrder: handleVoidOrder,
        }}
        loading={{
          changingStatus: isChangingStatus,
          rollingBackDraft: isRollingBackDraft,
          calculatingFee: isCalculatingFee,
          voidingOrder: isVoidingOrder,
        }}
      />

      {printOpen && (
        <PrintIssueDrawer
          detail={detail}
          open={printOpen}
          onClose={() => setPrintOpen(false)}
          onPrinted={refreshDetail}
        />
      )}
      <SnapshotDiffModal uuid={orderUuid} open={diffOpen} onClose={() => setDiffOpen(false)} />
      <FinishRollManageDrawer
        orderUuid={orderUuid}
        open={manageRollOpen}
        onClose={() => setManageRollOpen(false)}
        onSuccess={refreshDetail}
      />
    </>
  )
}

import { useState } from 'react'
import { Input, Modal, message } from 'antd'
import { notifyErrorOnce } from '../../../api/request'
import { useQueryClient } from '@tanstack/react-query'
import { useLocation, useNavigate } from 'react-router'
import { PERMISSIONS } from '../../../constants/permissions'
import { useHasPermission } from '../../../stores/authStore'
import type { PrintDTO, ProcessOrderDetailVO } from '../../../types/processOrder'
import FinishRollManageDrawer from '../../../pages/processOrder/FinishRollManageDrawer'
import SnapshotDiffModal from '../../../pages/processOrder/SnapshotDiffModal'
import { useCalcProcessOrderFee } from '../hooks/useCalcProcessOrderFee'
import { useChangeOrderStatus } from '../hooks/useChangeOrderStatus'
import { useCompleteProcessing } from '../hooks/useCompleteProcessing'
import { usePrintAndCompleteProcessOrder } from '../hooks/usePrintAndCompleteProcessOrder'
import { useRollbackProcessOrderToDraft } from '../hooks/useRollbackProcessOrderToDraft'
import { useVoidProcessOrder } from '../hooks/useVoidProcessOrder'
import { usePrepareProcessOrderReissue } from '../hooks/usePrepareProcessOrderReissue'
import { invalidateProcessOrderReadModels } from '../hooks/invalidateProcessOrderReadModels'
import { confirmOrderStatusChange, isRollbackStatusChange } from '../confirmOrderStatusChange'
import OrderExecutionPanel from './OrderExecutionPanel'
import PrintIssueDrawer from './PrintIssueDrawer'
import { processOrderReturnTarget } from '../../../pages/processOrder/processOrderNavigation'

interface Props {
  detail?: ProcessOrderDetailVO
}

export default function OrderExecutionHost({ detail }: Props) {
  const navigate = useNavigate()
  const location = useLocation()
  const queryClient = useQueryClient()
  const orderUuid = detail?.order.uuid
  const [printOpen, setPrintOpen] = useState(false)
  const [diffOpen, setDiffOpen] = useState(false)
  const [manageRollOpen, setManageRollOpen] = useState(false)
  const { mutateAsync: changeStatus, isPending: isChangingStatus } = useChangeOrderStatus()
  const { mutateAsync: completeProcessing, isPending: isCompletingProcessing } = useCompleteProcessing()
  const { mutateAsync: printAndCompleteProcessing, isPending: isConfirmingPrint } = usePrintAndCompleteProcessOrder(orderUuid)
  const { mutateAsync: rollbackToDraft, isPending: isRollingBackDraft } = useRollbackProcessOrderToDraft()
  const { mutateAsync: calcFee, isPending: isCalculatingFee } = useCalcProcessOrderFee(orderUuid)
  const { mutateAsync: voidOrder, isPending: isVoidingOrder } = useVoidProcessOrder()
  const { mutateAsync: prepareReissue, isPending: isPreparingReissue } = usePrepareProcessOrderReissue()
  const capabilities = {
    canManageOrder: useHasPermission(PERMISSIONS.orderManage),
    canCreateOrder: useHasPermission(PERMISSIONS.orderCreate),
    canBackRecord: useHasPermission(PERMISSIONS.orderBackRecord),
    canManageDelivery: useHasPermission(PERMISSIONS.deliveryManage),
    canManageSettlement: useHasPermission(PERMISSIONS.settleManage),
  }

  if (!detail || !orderUuid) return null
  const returnTo = processOrderReturnTarget(location.state, `/process-orders/${detail.order.uuid}`)

  const refreshDetail = async () => {
    await invalidateProcessOrderReadModels(queryClient, orderUuid)
  }

  const handleChangeStatus = async (targetStatus: number, reason?: string) => {
    if (targetStatus === 0) {
      await rollbackToDraft({ orderUuid, reason: reason ?? '' })
    } else if (detail.order.orderStatus === 2 && targetStatus === 3) {
      await completeProcessing({ orderUuid, reason })
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

  const handleFirstPrintConfirmed = (dto?: PrintDTO) =>
    printAndCompleteProcessing(dto)

  const handleConfirmPrintAndToRecord = () => {
    Modal.confirm({
      title: '确认打印并转待回录',
      content: '请确认纸张已从打印机输出。浏览器打印窗口仅代表人工确认，不代表打印机设备回执。',
      okText: '确认打印并转待回录',
      cancelText: '取消',
      onOk: async () => {
        try {
          await printAndCompleteProcessing(undefined)
          message.success('已确认打印并转入待回录')
        } catch (error) {
          notifyErrorOnce(error, '打印确认失败，请刷新后重试')
          await refreshDetail()
        }
      },
    })
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

  const handlePrepareReissue = () => {
    const expectedVersion = detail.order.version
    if (expectedVersion == null) {
      message.error('当前加工单缺少版本信息，请刷新后重试')
      return
    }
    const requestId = crypto.randomUUID()
    confirmOrderStatusChange({
      title: '申请变更并重新下发',
      orderNo: detail.order.orderNo,
      okText: '确认申请变更',
      reasonPlaceholder: '请填写本次下发后变更原因，便于审计追溯',
      requireReason: true,
      onConfirm: async (reason) => {
        await prepareReissue({
          orderUuid,
          requestId,
          expectedVersion,
          reason: reason ?? '',
        })
        message.success('变更申请已提交，订单已回到待下发，请编辑工艺后重新下发')
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
          onConfirmPrintAndToRecord: handleConfirmPrintAndToRecord,
          onPrepareReissue: handlePrepareReissue,
          onBackRecord: () => navigate(`/process-orders/${orderUuid}/back-record`),
          onSnapshotDiff: () => setDiffOpen(true),
          onManageRolls: () => setManageRollOpen(true),
          onEditDraft: () => navigate(`/process-orders/create?draft=${orderUuid}`),
          onChangeStatus: handleConfirmStatus,
          onCalcFee: handleCalcFee,
          onGoDelivery: () => {
            const finishUuids = (detail.finishRolls ?? []).map((finish) => finish.uuid)
            navigate(`/delivery-orders/create?customerUuid=${encodeURIComponent(detail.order.customerUuid ?? '')}`, {
              state: { finishUuids, from: returnTo },
            })
          },
          onGoSettle: () => navigate('/settle-orders/create', {
            state: { initialOrderUuids: [detail.order.uuid], from: returnTo },
          }),
          onVoidOrder: handleVoidOrder,
        }}
        loading={{
          changingStatus: isChangingStatus || isCompletingProcessing,
          confirmingPrint: isConfirmingPrint,
          rollingBackDraft: isRollingBackDraft,
          preparingReissue: isPreparingReissue,
          calculatingFee: isCalculatingFee,
          voidingOrder: isVoidingOrder,
        }}
      />

      {printOpen && (
        <PrintIssueDrawer
          detail={detail}
          open={printOpen}
          onClose={() => setPrintOpen(false)}
          onPrintConfirmed={handleFirstPrintConfirmed}
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

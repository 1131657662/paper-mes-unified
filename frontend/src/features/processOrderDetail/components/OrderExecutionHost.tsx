import { useState } from 'react'
import { message } from 'antd'
import { useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import FinishRollManageDrawer from '../../../pages/processOrder/FinishRollManageDrawer'
import SnapshotDiffModal from '../../../pages/processOrder/SnapshotDiffModal'
import { queries } from '../../../queries'
import { useCalcProcessOrderFee } from '../hooks/useCalcProcessOrderFee'
import { useChangeOrderStatus } from '../hooks/useChangeOrderStatus'
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
  const { mutateAsync: calcFee, isPending: isCalculatingFee } = useCalcProcessOrderFee(orderUuid)

  if (!detail || !orderUuid) return null

  const refreshDetail = async () => {
    await queryClient.invalidateQueries({
      queryKey: queries.processOrderDetail.detail(orderUuid).queryKey,
    })
  }

  const handleChangeStatus = async (targetStatus: number) => {
    await changeStatus({ orderUuid, targetStatus })
    message.success('状态已更新')
  }

  const handleCalcFee = async () => {
    const result = await calcFee()
    message.success(`计费已更新，总额 ¥${result.totalAmount ?? 0}`)
  }

  return (
    <>
      <OrderExecutionPanel
        detail={detail}
        actions={{
          onPrint: () => setPrintOpen(true),
          onBackRecord: () => navigate(`/process-orders/${orderUuid}/back-record`),
          onSnapshotDiff: () => setDiffOpen(true),
          onManageRolls: () => setManageRollOpen(true),
          onEditDraft: () => navigate(`/process-orders/create?draft=${orderUuid}`),
          onChangeStatus: handleChangeStatus,
          onCalcFee: handleCalcFee,
          onGoDelivery: () => navigate('/delivery-orders'),
          onGoSettle: () => navigate('/settle-orders'),
        }}
        loading={{
          changingStatus: isChangingStatus,
          calculatingFee: isCalculatingFee,
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

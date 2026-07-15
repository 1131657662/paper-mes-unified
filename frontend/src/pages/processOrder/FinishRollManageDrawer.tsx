import { Drawer, Spin } from 'antd'
import { useProcessOrderDetail } from '../../features/processOrderDetail/hooks/useProcessOrderDetail'
import FinishRollGenerateModal from './FinishRollGenerateModal'
import FinishRollManagerView from './FinishRollManagerView'
import FinishRollSpareModal from './FinishRollSpareModal'
import { useFinishRollManagerSession } from './useFinishRollManagerSession'
import './FinishRollManageDrawer.css'

interface Props {
  onClose: () => void
  onSuccess: () => void
  open: boolean
  orderUuid: string | null
}

export default function FinishRollManageDrawer({ onClose, onSuccess, open, orderUuid }: Props) {
  const { data: detail, isLoading: isLoadingDetail, refetch } = useProcessOrderDetail(orderUuid ?? undefined, { enabled: open })
  const title = `成品卷号管理${detail?.order.orderNo ? `（${detail.order.orderNo}）` : ''}`
  return (
    <Drawer className="mes-detail-drawer finish-roll-drawer" destroyOnHidden open={open} title={title} width={1120} onClose={onClose}>
      {open && orderUuid && (
        <Spin spinning={isLoadingDetail}>
          <FinishRollManagerSession key={orderUuid} detail={detail} orderUuid={orderUuid} refetch={refetch} onSuccess={onSuccess} />
        </Spin>
      )}
    </Drawer>
  )
}

interface SessionProps {
  detail: ReturnType<typeof useProcessOrderDetail>['data']
  onSuccess: () => void
  orderUuid: string
  refetch: ReturnType<typeof useProcessOrderDetail>['refetch']
}

function FinishRollManagerSession({ detail, onSuccess, orderUuid, refetch }: SessionProps) {
  const manager = useFinishRollManagerSession({ onSuccess, orderUuid, refetch })
  const sourceOptions = (detail?.originalRolls ?? [])
    .filter((roll) => roll.processMode !== 3)
    .map((roll, index) => ({
      label: `母卷 ${index + 1} / ${roll.rollNo || roll.extraNo || '未记录卷号'} / ${roll.paperName || '-'}`,
      value: roll.uuid,
    }))
  return (
    <>
      <FinishRollManagerView
        actions={{ onAppendSpare: () => manager.setDialog('spare'), onBatchVoid: manager.voidSelected, onChangeFilters: manager.setFilters, onGenerate: () => manager.setDialog('generate'), onSelectionChange: manager.setSelectedKeys, onVoid: manager.voidSingle }}
        detail={detail}
        state={{ filters: manager.filters, selectedKeys: manager.selectedKeys }}
      />
      <FinishRollGenerateModal open={manager.dialog === 'generate'} sourceOptions={sourceOptions} onCancel={() => manager.setDialog(undefined)} onSubmit={manager.generate} />
      <FinishRollSpareModal open={manager.dialog === 'spare'} sourceOptions={sourceOptions} onCancel={() => manager.setDialog(undefined)} onSubmit={manager.appendSpare} />
    </>
  )
}

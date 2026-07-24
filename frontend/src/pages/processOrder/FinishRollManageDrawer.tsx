import { Drawer, Spin } from 'antd'
import { useQueryClient } from '@tanstack/react-query'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { invalidateProcessOrderReadModels } from '../../features/processOrderDetail/hooks/invalidateProcessOrderReadModels'
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
  const queryClient = useQueryClient()
  const {
    data: detail,
    isError: isDetailError,
    isLoading: isLoadingDetail,
    refetch,
  } = useProcessOrderDetail(orderUuid ?? undefined, { enabled: open })
  const refreshOrderViews = () => orderUuid
    ? invalidateProcessOrderReadModels(queryClient, orderUuid)
    : Promise.resolve()
  const title = `成品卷号管理${detail?.order.orderNo ? `（${detail.order.orderNo}）` : ''}`
  return (
    <Drawer className="mes-detail-drawer finish-roll-drawer" destroyOnHidden open={open} title={title}
      width="min(1120px, 96vw)" onClose={onClose}>
      {open && orderUuid && (
        <Spin spinning={isLoadingDetail}>
          {isDetailError ? (
            <QueryLoadErrorAlert
              message="成品卷号信息加载失败"
              description="当前空白不代表没有成品卷号，请重新加载后再操作。"
              onRetry={() => void refetch()}
            />
          ) : (
            <FinishRollManagerSession key={orderUuid} detail={detail} orderUuid={orderUuid}
              refetch={refreshOrderViews} onSuccess={onSuccess} />
          )}
        </Spin>
      )}
    </Drawer>
  )
}

interface SessionProps {
  detail: ReturnType<typeof useProcessOrderDetail>['data']
  onSuccess: () => void
  orderUuid: string
  refetch: () => Promise<unknown>
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

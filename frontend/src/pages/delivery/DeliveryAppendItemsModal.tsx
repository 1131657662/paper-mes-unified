import { useEffect, useState } from 'react'
import { Modal, message } from 'antd'
import { useAppendDeliveryDetails } from '../../features/delivery/hooks/useAppendDeliveryDetails'
import { useAvailableFinishes } from '../../features/delivery/hooks/useAvailableFinishes'
import { availableFinishWeight, formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'
import DeliveryCreateTable, { type DeliveryLineEdit } from './DeliveryCreateTable'

interface Props {
  customerName?: string
  customerUuid?: string
  deliveryUuid?: string
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function DeliveryAppendItemsModal({
  customerName,
  customerUuid,
  deliveryUuid,
  onClose,
  onSuccess,
  open,
}: Props) {
  const appendMutation = useAppendDeliveryDetails()
  const finishesQuery = useAvailableFinishes(open ? customerUuid : undefined)
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [lineEdits, setLineEdits] = useState<Record<string, DeliveryLineEdit>>({})
  const finishes = finishesQuery.data ?? []
  const selectedFinishes = finishes.filter((item) => selectedRowKeys.includes(item.finishUuid))

  useEffect(() => {
    if (!open) {
      setSelectedRowKeys([])
      setLineEdits({})
    }
  }, [open])

  const handleEditChange = (finishUuid: string, value: DeliveryLineEdit) => {
    setLineEdits((prev) => ({ ...prev, [finishUuid]: { ...prev[finishUuid], ...value } }))
  }

  const handleSubmit = async () => {
    if (!deliveryUuid) return
    if (selectedFinishes.length === 0) {
      message.warning('请先勾选要追加的成品卷')
      return
    }
    const hasRisk = selectedFinishes.some((item) => item.settlementRisk)
    if (hasRisk) {
      await confirmCashRelease()
    }
    await appendMutation.mutateAsync({
      uuid: deliveryUuid,
      data: buildAppendDTO(selectedFinishes, lineEdits, hasRisk),
    })
    message.success('已追加到本张出库单')
    onSuccess()
    onClose()
  }

  return (
    <Modal
      destroyOnHidden
      title="添加出库卷"
      open={open}
      width={1080}
      okText="追加到本单"
      cancelText="取消"
      confirmLoading={appendMutation.isPending}
      onCancel={onClose}
      onOk={handleSubmit}
    >
      <div className="delivery-append-modal">
        <div className="document-module-summary">
          <span>客户 <strong>{customerName || '-'}</strong></span>
          <span>可选 <strong>{finishes.length}</strong> 卷</span>
          <span>已选 <strong>{selectedFinishes.length}</strong> 卷</span>
          <span>合计 <strong>{formatTon(selectedWeight(selectedFinishes, lineEdits))}</strong></span>
        </div>
        <div className="document-module-table">
          <DeliveryCreateTable
            data={finishes}
            edits={lineEdits}
            loading={finishesQuery.isLoading || finishesQuery.isFetching}
            selectedRowKeys={selectedRowKeys}
            onEditChange={handleEditChange}
            onSelectionChange={setSelectedRowKeys}
          />
        </div>
      </div>
    </Modal>
  )
}

function buildAppendDTO(
  selectedFinishes: AvailableFinishVO[],
  lineEdits: Record<string, DeliveryLineEdit>,
  forceRelease: boolean,
) {
  return {
    forceRelease,
    items: selectedFinishes.map((item) => ({
      finishUuid: item.finishUuid,
      outWeight: lineEdits[item.finishUuid]?.outWeight,
      remark: lineEdits[item.finishUuid]?.remark,
    })),
  }
}

function selectedWeight(items: AvailableFinishVO[], edits: Record<string, DeliveryLineEdit>) {
  return items.reduce((total, item) => total + (edits[item.finishUuid]?.outWeight ?? availableFinishWeight(item)), 0)
}

function confirmCashRelease() {
  return new Promise<void>((resolve, reject) => {
    Modal.confirm({
      title: '次结出库确认',
      content: '本次追加包含次结且有待收款风险的加工单。确认后将按“警告放行”追加到本张出库单。',
      okText: '警告放行',
      cancelText: '取消',
      onOk: () => resolve(),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}

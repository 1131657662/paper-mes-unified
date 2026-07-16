import { useState } from 'react'
import { Modal, Space, message } from 'antd'
import { useAppendDeliveryDetails } from '../../features/delivery/hooks/useAppendDeliveryDetails'
import { useAvailableFinishes } from '../../features/delivery/hooks/useAvailableFinishes'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'
import DeliveryCreateTable from './DeliveryCreateTable'
import DeliveryFinishTableToolbar from './DeliveryFinishTableToolbar'
import {
  DeliveryFinishScopeControl,
} from './DeliveryFinishScopeControl'
import { filterFinishesByScope, finishScopeName, type DeliveryFinishScope } from './deliveryFinishScope'
import {
  defaultDeliveryFinishFilters,
  filterDeliveryFinishes,
  type DeliveryFinishFilters,
} from './deliveryFinishFilter'
import {
  deliverySelectionError,
  selectedDeliveryFinishes,
  summarizeDeliverySelection,
  type DeliveryLineEdit,
} from './deliverySelectionModel'

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
  const [finishScope, setFinishScope] = useState<DeliveryFinishScope>('product')
  const [finishFilters, setFinishFilters] = useState<DeliveryFinishFilters>({ ...defaultDeliveryFinishFilters })
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const [lineEdits, setLineEdits] = useState<Record<string, DeliveryLineEdit>>({})
  const finishes = finishesQuery.data ?? []
  const scopedFinishes = filterFinishesByScope(finishes, finishScope)
  const visibleFinishes = filterDeliveryFinishes(scopedFinishes, finishFilters, selectedRowKeys)
  const selectedFinishes = selectedDeliveryFinishes(finishes, selectedRowKeys)
  const selectionSummary = summarizeDeliverySelection(selectedFinishes, lineEdits)
  const selectionError = deliverySelectionError(selectedFinishes, lineEdits)
  const scopeName = finishScopeName(finishScope)
  const emptyText = scopedFinishes.length === 0
    ? `该客户暂无可出库${scopeName}`
    : `没有符合筛选条件的${scopeName}`

  const handleScopeChange = (value: DeliveryFinishScope) => {
    setFinishScope(value)
  }

  const resetSelection = () => {
    setFinishScope('product')
    setFinishFilters({ ...defaultDeliveryFinishFilters })
    setSelectedRowKeys([])
    setLineEdits({})
  }

  const handleEditChange = (finishUuid: string, value: DeliveryLineEdit) => {
    setLineEdits((prev) => ({ ...prev, [finishUuid]: { ...prev[finishUuid], ...value } }))
  }

  const handleSubmit = async () => {
    if (!deliveryUuid) return
    if (selectedFinishes.length === 0) {
      message.warning('请先勾选要追加的成品或余料')
      return
    }
    if (selectionError) {
      message.error(selectionError)
      return
    }
    const hasRisk = selectedFinishes.some((item) => item.settlementRisk)
    if (hasRisk) {
      const confirmed = await confirmCashRelease()
      if (!confirmed) return
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
      okButtonProps={{ disabled: Boolean(selectionError) || selectedFinishes.length === 0 }}
      afterClose={resetSelection}
      onCancel={onClose}
      onOk={handleSubmit}
    >
      <div className="delivery-append-modal">
        <Space className="document-module-summary" size={12} wrap>
          <span>客户 <strong>{customerName || '-'}</strong></span>
          <DeliveryFinishScopeControl
            finishes={finishes}
            selectedRowKeys={selectedRowKeys}
            value={finishScope}
            onChange={handleScopeChange}
          />
          <span>可选 <strong>{visibleFinishes.length}</strong> 卷</span>
          <span>已选 <strong>{selectionSummary.totalCount}</strong> 卷</span>
          <span>成品 <strong>{selectionSummary.productCount}</strong> 卷</span>
          <span>余料 <strong>{selectionSummary.remainCount}</strong> 卷</span>
          <span>合计 <strong>{formatTon(selectionSummary.totalWeight)}</strong></span>
        </Space>
        <DeliveryFinishTableToolbar
          filters={finishFilters}
          resultCount={visibleFinishes.length}
          scope={finishScope}
          totalCount={scopedFinishes.length}
          onChange={setFinishFilters}
        />
        <div className="document-module-table">
          <DeliveryCreateTable
            data={visibleFinishes}
            edits={lineEdits}
            emptyText={emptyText}
            loading={finishesQuery.isLoading || finishesQuery.isFetching}
            scope={finishScope}
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

function confirmCashRelease() {
  return new Promise<boolean>((resolve) => {
    Modal.confirm({
      title: '次结出库确认',
      content: '本次追加包含次结且有待收款风险的加工单。确认后将按“警告放行”追加到本张出库单。',
      okText: '警告放行',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

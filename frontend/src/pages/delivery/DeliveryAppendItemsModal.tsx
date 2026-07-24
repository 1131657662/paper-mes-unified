import { useState } from 'react'
import { Modal, Space, message } from 'antd'
import { useAppendDeliveryDetails } from '../../features/delivery/hooks/useAppendDeliveryDetails'
import DocumentPaginationBar from '../../components/biz/DocumentPaginationBar'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { AvailableFinishVO } from '../../types/delivery'
import DeliveryCreateTable from './DeliveryCreateTable'
import DeliveryFinishTableToolbar from './DeliveryFinishTableToolbar'
import {
  DeliveryFinishScopeControl,
} from './DeliveryFinishScopeControl'
import { finishScopeName, type DeliveryFinishScope } from './deliveryFinishScope'
import {
  deliverySelectionError,
  summarizeDeliverySelection,
  type DeliveryLineEdit,
} from './deliverySelectionModel'
import { useDeliveryAppendInventory } from './useDeliveryAppendInventory'

interface Props {
  customerName?: string
  customerUuid?: string
  warehouseUuid?: string
  deliveryUuid?: string
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function DeliveryAppendItemsModal({
  customerName,
  customerUuid,
  warehouseUuid,
  deliveryUuid,
  onClose,
  onSuccess,
  open,
}: Props) {
  const appendMutation = useAppendDeliveryDetails()
  const inventory = useDeliveryAppendInventory({ customerUuid, warehouseUuid, enabled: open })
  const [lineEdits, setLineEdits] = useState<Record<string, DeliveryLineEdit>>({})
  const selectedFinishes = inventory.selectedRows
  const selectionSummary = summarizeDeliverySelection(selectedFinishes, lineEdits)
  const selectionError = deliverySelectionError(selectedFinishes, lineEdits)
  const scopeName = finishScopeName(inventory.scope)
  const emptyText = (inventory.query.data?.total ?? 0) === 0
    ? `该客户暂无可出库${scopeName}`
    : `没有符合筛选条件的${scopeName}`

  const handleScopeChange = (value: DeliveryFinishScope) => {
    inventory.changeScope(value)
  }

  const resetSelection = () => {
    inventory.reset()
    setLineEdits({})
  }

  const handleEditChange = (finishUuid: string, value: DeliveryLineEdit) => {
    setLineEdits((prev) => ({ ...prev, [finishUuid]: { ...prev[finishUuid], ...value } }))
  }

  const handleSubmit = async () => {
    if (appendMutation.isPending) return
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
        {inventory.query.isError && (
          <QueryLoadErrorAlert
            message="可追加库存加载失败"
            description="当前空表不代表没有可追加库存，请重新加载后再选择。"
            onRetry={() => void inventory.query.refetch()}
          />
        )}
        <Space className="document-module-summary" size={12} wrap>
          <span>客户 <strong>{customerName || '-'}</strong></span>
          <DeliveryFinishScopeControl
            finishes={inventory.selectionPool}
            selectedRowKeys={inventory.selectedKeys}
            scopeTotals={inventory.query.data?.scopeCounts}
            totalCount={inventory.query.data?.total}
            value={inventory.scope}
            onChange={handleScopeChange}
          />
          <span>可选 <strong>{inventory.query.data?.total ?? 0}</strong> 卷</span>
          <span>已选 <strong>{selectionSummary.totalCount}</strong> 卷</span>
          <span>成品 <strong>{selectionSummary.productCount}</strong> 卷</span>
          <span>余料 <strong>{selectionSummary.remainCount}</strong> 卷</span>
          <span>合计 <strong>{formatTon(selectionSummary.totalWeight)}</strong></span>
        </Space>
        <DeliveryFinishTableToolbar
          filters={inventory.filters}
          resultCount={inventory.visibleRows.length}
          scope={inventory.scope}
          totalCount={inventory.query.data?.total ?? 0}
          onChange={inventory.changeFilters}
        />
        <div className="document-module-table">
          <DeliveryCreateTable
            data={inventory.visibleRows}
            edits={lineEdits}
            emptyText={emptyText}
            loading={inventory.query.isLoading || inventory.query.isFetching}
            scope={inventory.scope}
            selectedRowKeys={inventory.selectedKeys}
            onEditChange={handleEditChange}
            onSelectionChange={inventory.changeSelection}
          />
        </div>
        {!inventory.filters.selectedOnly && (
          <DocumentPaginationBar
            current={inventory.page}
            pageSize={inventory.pageSize}
            total={inventory.query.data?.total ?? 0}
            onChange={inventory.changePage}
          />
        )}
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
      title: '现结出库确认',
      content: '本次追加包含现结且有待收款风险的加工单。确认后将按“警告放行”追加到本张出库单。',
      okText: '警告放行',
      cancelText: '取消',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

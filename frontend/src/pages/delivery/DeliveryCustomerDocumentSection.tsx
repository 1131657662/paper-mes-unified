import { Card, Segmented, Space } from 'antd'
import { useMemo, useState } from 'react'
import SortClearControl from '../../components/biz/SortClearControl'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import DeliveryCustomerDocumentStrip from '../../features/deliveryCustomerSpec/DeliveryCustomerDocumentStrip'
import DeliveryCustomerRevisionDrawer from '../../features/deliveryCustomerSpec/DeliveryCustomerRevisionDrawer'
import DeliveryCustomerRevisionHistoryDrawer from '../../features/deliveryCustomerSpec/DeliveryCustomerRevisionHistoryDrawer'
import DeliveryCustomerViewTable from '../../features/deliveryCustomerSpec/DeliveryCustomerViewTable'
import type { DeliveryCustomerRevisionPreview, DeliveryDocumentView } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetailVO, DeliveryDetail } from '../../types/delivery'
import { buildDeliveryDetailColumns } from './deliveryDetailColumns'
import { sortDeliveryDetails } from './deliveryDetailSorting'
import type { useDeliveryDetailTableState } from './useDeliveryDetailTableState'
import { resolveDeliveryOverview } from './deliveryDetailState'

interface Props {
  canManage: boolean
  customerSpecs?: DeliveryCustomerRevisionPreview
  detail: DeliveryDetailVO
  loading?: boolean
  view: DeliveryDocumentView
  sortState: ReturnType<typeof useDeliveryDetailTableState>
  onReload: () => void
  onRemove: (record: DeliveryDetail) => void
  onViewChange: (view: DeliveryDocumentView) => void
}

export default function DeliveryCustomerDocumentSection(props: Props) {
  const [editorOpen, setEditorOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const { detail, customerSpecs, view } = props
  const deliveryState = resolveDeliveryOverview(detail.order)
  const physicalState = props.sortState.physical
  const sortedDetails = useMemo(
    () => sortDeliveryDetails(detail.details, physicalState.sortChain),
    [detail.details, physicalState.sortChain],
  )
  const activeSortChain = view === 'physical'
    ? physicalState.sortChain
    : view === 'customer' ? props.sortState.customer.sortChain : props.sortState.trace.sortChain
  const clearActiveSort = view === 'physical'
    ? physicalState.clearSort
    : view === 'customer' ? props.sortState.customer.clearSort : props.sortState.trace.clearSort

  return (
    <Card className="document-module-card" title="出库明细" extra={
      <Space size={8}>
        <Segmented<DeliveryDocumentView> aria-label="出库明细视图" options={[
          { label: '客户单据', value: 'customer' },
          { label: '仓库实物', value: 'physical' },
          { label: '追溯对照', value: 'trace' },
        ]} value={view} onChange={props.onViewChange} />
        <SortClearControl activeCount={activeSortChain.length} onClear={clearActiveSort} />
      </Space>
    }>
      <DeliveryCustomerDocumentStrip canEdit={props.canManage && deliveryState.canEdit} data={customerSpecs} deliveryStatus={detail.order.deliveryStatus} loading={props.loading} onEdit={() => setEditorOpen(true)} onHistory={() => setHistoryOpen(true)} />
      <div className="document-module-table">
        {view === 'physical' ? (
          <DocumentDetailTable
            storageKey="delivery-detail-items"
            rowKey="uuid"
            columns={buildDeliveryDetailColumns({ canRemove: props.canManage, deliveryStatus: detail.order.deliveryStatus, onRemove: props.onRemove, sortChain: physicalState.sortChain })}
            dataSource={sortedDetails}
            onChange={physicalState.onChange}
            onReload={props.onReload}
            pagination={false}
            scroll={{ x: 1280 }}
          />
        ) : view === 'customer' ? (
          <DeliveryCustomerViewTable
            details={detail.details}
            items={customerSpecs?.items}
            view="customer"
            sortChain={props.sortState.customer.sortChain}
            onChange={props.sortState.customer.onChange}
          />
        ) : (
          <DeliveryCustomerViewTable
            details={detail.details}
            items={customerSpecs?.items}
            view="trace"
            sortChain={props.sortState.trace.sortChain}
            onChange={props.sortState.trace.onChange}
          />
        )}
      </div>
      {editorOpen && customerSpecs && <DeliveryCustomerRevisionDrawer data={customerSpecs} open uuid={detail.order.uuid} onClose={() => setEditorOpen(false)} />}
      <DeliveryCustomerRevisionHistoryDrawer open={historyOpen} uuid={detail.order.uuid} onClose={() => setHistoryOpen(false)} />
    </Card>
  )
}

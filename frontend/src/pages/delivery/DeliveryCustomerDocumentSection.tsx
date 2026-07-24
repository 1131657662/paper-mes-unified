import { Card, Segmented } from 'antd'
import { useState } from 'react'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import DeliveryCustomerDocumentStrip from '../../features/deliveryCustomerSpec/DeliveryCustomerDocumentStrip'
import DeliveryCustomerRevisionDrawer from '../../features/deliveryCustomerSpec/DeliveryCustomerRevisionDrawer'
import DeliveryCustomerRevisionHistoryDrawer from '../../features/deliveryCustomerSpec/DeliveryCustomerRevisionHistoryDrawer'
import DeliveryCustomerViewTable from '../../features/deliveryCustomerSpec/DeliveryCustomerViewTable'
import type { DeliveryCustomerRevisionPreview, DeliveryDocumentView } from '../../features/deliveryCustomerSpec/deliveryCustomerSpecTypes'
import type { DeliveryDetailVO, DeliveryDetail } from '../../types/delivery'
import { buildDeliveryDetailColumns } from './deliveryDetailColumns'

interface Props {
  canManage: boolean
  customerSpecs?: DeliveryCustomerRevisionPreview
  detail: DeliveryDetailVO
  loading?: boolean
  view: DeliveryDocumentView
  onReload: () => void
  onRemove: (record: DeliveryDetail) => void
  onViewChange: (view: DeliveryDocumentView) => void
}

export default function DeliveryCustomerDocumentSection(props: Props) {
  const [editorOpen, setEditorOpen] = useState(false)
  const [historyOpen, setHistoryOpen] = useState(false)
  const { detail, customerSpecs, view } = props
  return (
    <Card className="document-module-card" title="出库明细" extra={
      <Segmented<DeliveryDocumentView> aria-label="出库明细视图" options={[
        { label: '客户单据', value: 'customer' },
        { label: '仓库实物', value: 'physical' },
        { label: '追溯对照', value: 'trace' },
      ]} value={view} onChange={props.onViewChange} />
    }>
      <DeliveryCustomerDocumentStrip canEdit={props.canManage && detail.order.deliveryStatus !== 3} data={customerSpecs} deliveryStatus={detail.order.deliveryStatus} loading={props.loading} onEdit={() => setEditorOpen(true)} onHistory={() => setHistoryOpen(true)} />
      <div className="document-module-table">
        {view === 'physical' ? <DocumentDetailTable storageKey="delivery-detail-items" rowKey="uuid" columns={buildDeliveryDetailColumns({ canRemove: props.canManage, deliveryStatus: detail.order.deliveryStatus, onRemove: props.onRemove })} dataSource={detail.details} onReload={props.onReload} pagination={false} scroll={{ x: 1280 }} /> : <DeliveryCustomerViewTable details={detail.details} items={customerSpecs?.items} view={view} />}
      </div>
      {editorOpen && customerSpecs && <DeliveryCustomerRevisionDrawer data={customerSpecs} open uuid={detail.order.uuid} onClose={() => setEditorOpen(false)} />}
      <DeliveryCustomerRevisionHistoryDrawer open={historyOpen} uuid={detail.order.uuid} onClose={() => setHistoryOpen(false)} />
    </Card>
  )
}

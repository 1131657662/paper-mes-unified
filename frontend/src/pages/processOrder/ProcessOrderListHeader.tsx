import { Button, Card } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import type { ProcessOrder } from '../../types/processOrder'
import ProcessOrderBatchToolbar, { type BatchActions } from './ProcessOrderBatchToolbar'
import ProcessOrderQueueBar, { type QueueStatus } from './ProcessOrderQueueBar'

interface Props {
  quickStatus: QueueStatus
  selectedRows: ProcessOrder[]
  actions: BatchActions
  children: React.ReactNode
  extra?: React.ReactNode
  search?: React.ReactNode
  onCreate: () => void
  onQuickStatusChange: (value: QueueStatus) => void
}

export default function ProcessOrderListHeader({
  actions,
  children,
  extra,
  onCreate,
  onQuickStatusChange,
  quickStatus,
  search,
  selectedRows,
}: Props) {
  return (
    <Card
      title="加工单"
      className="process-order-shell"
    >
      {search && <div className="process-order-shell__search">{search}</div>}
      <div className="process-order-shell__toolbar">
        <div className="process-order-shell__actions">
          <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>新建</Button>
          <ProcessOrderBatchToolbar selectedRows={selectedRows} actions={actions} />
        </div>
        <div className="process-order-shell__queue">
          <ProcessOrderQueueBar value={quickStatus} onChange={onQuickStatusChange} />
        </div>
        {extra && <div className="process-order-shell__toolbar-tools">{extra}</div>}
      </div>
      {children}
    </Card>
  )
}

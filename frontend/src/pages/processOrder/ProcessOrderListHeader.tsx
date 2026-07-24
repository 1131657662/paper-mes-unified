import { useState } from 'react'
import { Button, Card } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { TableToolbarHostProvider } from '../../components/biz/TableToolbarPortal'
import type { ProcessOrder } from '../../types/processOrder'
import ProcessOrderBatchToolbar, { type BatchActions } from './ProcessOrderBatchToolbar'
import ProcessOrderQueueBar, { type QueueStatus } from './ProcessOrderQueueBar'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

interface Props {
  quickStatus: QueueStatus
  selectedRows: ProcessOrder[]
  actions: BatchActions
  capabilities: ProcessOrderListCapabilities
  children: React.ReactNode
  search?: React.ReactNode
  onCreate: () => void
  onQuickStatusChange: (value: QueueStatus) => void
}

export default function ProcessOrderListHeader({
  actions,
  capabilities,
  children,
  onCreate,
  onQuickStatusChange,
  quickStatus,
  search,
  selectedRows,
}: Props) {
  const [toolsHost, setToolsHost] = useState<HTMLDivElement | null>(null)

  return (
    <Card
      title="加工单"
      className="process-order-shell"
    >
      {search && <div className="process-order-shell__search">{search}</div>}
      <div className={`process-order-shell__toolbar${selectedRows.length > 0 ? ' has-selection' : ''}`}>
        <div className="process-order-shell__actions">
          {capabilities.canCreateOrder && <Button type="primary" icon={<PlusOutlined />} onClick={onCreate}>新建</Button>}
          <ProcessOrderBatchToolbar selectedRows={selectedRows} actions={actions} capabilities={capabilities} />
        </div>
        <div className="process-order-shell__queue">
          <ProcessOrderQueueBar value={quickStatus} onChange={onQuickStatusChange} />
        </div>
        <div className="process-order-shell__table-tools" ref={setToolsHost} />
      </div>
      <TableToolbarHostProvider host={toolsHost}>
        {children}
      </TableToolbarHostProvider>
    </Card>
  )
}

import { Button, Card } from 'antd'
import { MessageOutlined } from '@ant-design/icons'
import { useState } from 'react'
import type { ColumnsType } from 'antd/es/table'
import DocumentAuditTimeline from '../../components/biz/DocumentAuditTimeline'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import { PERMISSIONS } from '../../constants/permissions'
import { useSettleCollectionReminders } from '../../features/settle/hooks/useSettleCollectionReminders'
import { useHasPermission } from '../../stores/authStore'
import type { ReceiveRecord, SettleDetailVO } from '../../types/settle'
import CollectionReminderModal from './CollectionReminderModal'
import SettleCollectionReminderHistory from './SettleCollectionReminderHistory'
import SettleGroupedBill from './SettleGroupedBill'
import SettlePrintSheet from './SettlePrintSheet'
import SettlementInfoCard from './SettlementInfoCard'
import { buildSettleDetailColumns } from './settleDetailColumns'
import type { DetailTab } from './SettleDetailTabNav'

interface Props {
  activeTab: DetailTab
  detail: SettleDetailVO
  extraFeeByOrder: Record<string, string>
  receiveTableColumns: ColumnsType<ReceiveRecord>
  onReloadDetails: () => void
  onReloadReceives: () => void
}

export default function SettleDetailTabContent({
  activeTab,
  detail,
  extraFeeByOrder,
  receiveTableColumns,
  onReloadDetails,
  onReloadReceives,
}: Props) {
  if (activeTab === 'overview') {
    return <OverviewContent detail={detail} extraFeeByOrder={extraFeeByOrder} onReload={onReloadDetails} />
  }
  if (activeTab === 'receives') {
    return <ReceivesContent detail={detail} columns={receiveTableColumns} onReload={onReloadReceives} />
  }
  if (activeTab === 'audit') {
    return <AuditContent detail={detail} />
  }
  return <div>
    <Card className="document-module-card document-module-card--print" title="客户单据预览">
      <SettlePrintSheet detail={detail} />
    </Card>
  </div>
}

function AuditContent({ detail }: { detail: SettleDetailVO }) {
  const [reminderOpen, setReminderOpen] = useState(false)
  const canReceive = useHasPermission(PERMISSIONS.settleReceive)
  const remindersQuery = useSettleCollectionReminders(detail.order.uuid)
  const canRecord = canReceive && [1, 2].includes(detail.order.settleStatus)

  return <>
    <Card className="document-module-card" title="催收跟进"
      extra={canRecord && <Button type="primary" ghost icon={<MessageOutlined />} onClick={() => setReminderOpen(true)}>记录催收</Button>}>
      <SettleCollectionReminderHistory error={remindersQuery.isError} items={remindersQuery.data ?? []}
        loading={remindersQuery.isLoading} onRetry={() => void remindersQuery.refetch()} />
    </Card>
    <Card className="document-module-card" title="业务追踪">
      <DocumentAuditTimeline logs={detail.operationLogs ?? []} />
    </Card>
    <CollectionReminderModal record={reminderOpen ? detail.order : null} onClose={() => setReminderOpen(false)} />
  </>
}

function OverviewContent({ detail, extraFeeByOrder, onReload }: {
  detail: SettleDetailVO
  extraFeeByOrder: Record<string, string>
  onReload: () => void
}) {
  return <>
    <SettlementInfoCard detail={detail} />
    <Card className="document-module-card" title="加工单费用组成">
      <DocumentDetailTable
        storageKey="settle-detail-fee-items"
        rowKey="uuid"
        columns={buildSettleDetailColumns(extraFeeByOrder)}
        dataSource={detail.details}
        onReload={onReload}
        pagination={false}
        scroll={{ x: 680 }}
      />
    </Card>
    <Card className="document-module-card" title="客户结算明细">
      <SettleGroupedBill lines={detail.printLines ?? []} />
    </Card>
  </>
}

function ReceivesContent({ detail, columns, onReload }: {
  detail: SettleDetailVO
  columns: ColumnsType<ReceiveRecord>
  onReload: () => void
}) {
  return <Card className="document-module-card" title="收款记录">
    <DocumentDetailTable
      storageKey="settle-detail-receive-records"
      rowKey="uuid"
      columns={columns}
      dataSource={detail.receives}
      onReload={onReload}
      pagination={false}
      scroll={{ x: 1050 }}
    />
  </Card>
}

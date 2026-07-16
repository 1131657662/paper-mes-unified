import { Card, Descriptions, Tag } from 'antd'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail, DeliveryRollbackSnapshotVO } from '../../types/delivery'
import { buildDeliveryDetailColumns } from './deliveryDetailColumns'
import { formatDateTime } from '../../utils/dateTime'

interface DeliveryRollbackSnapshotCardProps {
  snapshot: DeliveryRollbackSnapshotVO
}

export default function DeliveryRollbackSnapshotCard({ snapshot }: DeliveryRollbackSnapshotCardProps) {
  const details = snapshot.details ?? []

  return (
    <Card
      className="document-module-card delivery-rollback-snapshot"
      title="回退前签收快照"
      extra={<Tag color="orange">历史签收版本</Tag>}
    >
      <Descriptions
        bordered
        className="delivery-rollback-snapshot__meta"
        column={{ xs: 1, md: 2, xl: 4 }}
        size="small"
      >
        <Descriptions.Item label="原出库单">{snapshot.deliveryNo || '-'}</Descriptions.Item>
        <Descriptions.Item label="原签收人">{snapshot.signUser || '-'}</Descriptions.Item>
        <Descriptions.Item label="原签收时间">{formatDateTime(snapshot.signTime)}</Descriptions.Item>
        <Descriptions.Item label="原签收统计">
          {snapshot.totalCount ?? details.length} 卷 / {formatTon(snapshot.totalWeight)}
        </Descriptions.Item>
        <Descriptions.Item label="回退人">{snapshot.rollbackOperator || '-'}</Descriptions.Item>
        <Descriptions.Item label="回退时间">{formatDateTime(snapshot.rollbackTime)}</Descriptions.Item>
        <Descriptions.Item label="回退原因" span="filled">{snapshot.rollbackReason || '-'}</Descriptions.Item>
      </Descriptions>

      <div className="document-module-table delivery-rollback-snapshot__table">
        <DocumentDetailTable<DeliveryDetail>
          storageKey="delivery-rollback-snapshot-items"
          rowKey={(record, index) => record.uuid || `${record.finishRollNo}-${index}`}
          columns={buildDeliveryDetailColumns({
            deliveryStatus: 2,
            onRemove: () => undefined,
          })}
          dataSource={details}
          pagination={false}
          scroll={{ x: 1280, y: 360 }}
        />
      </div>
    </Card>
  )
}

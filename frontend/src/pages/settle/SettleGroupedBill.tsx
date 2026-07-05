import { Collapse, Empty, Typography } from 'antd'
import type { ColumnGroupType, ColumnType, ColumnsType } from 'antd/es/table'
import DocumentDetailTable from '../../components/biz/DocumentDetailTable'
import { formatMoney, formatTon } from '../../features/settle/utils/settleFormatters'
import type { SettlePrintLine } from '../../types/settle'
import { buildSettleBillGroups, type SettleBillGroup } from './settleBillGroups'
import { settlePrintLineColumns } from './settleDetailColumns'

interface Props {
  lines: SettlePrintLine[]
}

export default function SettleGroupedBill({ lines }: Props) {
  const groups = buildSettleBillGroups(lines)
  if (groups.length === 0) {
    return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可结算明细" />
  }

  return (
    <Collapse
      className="settle-grouped-bill"
      defaultActiveKey={groups.map((group) => group.key)}
      items={groups.map((group) => ({
        key: group.key,
        label: <GroupHeader group={group} />,
        children: (
          <DocumentDetailTable
            storageKey={`settle-detail-bill-${group.key}`}
            rowKey={(record) => `${record.orderUuid}-${record.originalUuid}`}
            columns={groupLineColumns}
            dataSource={group.lines}
            pagination={false}
            scroll={{ x: 2620 }}
          />
        ),
      }))}
    />
  )
}

function GroupHeader({ group }: { group: SettleBillGroup }) {
  return (
    <div className="settle-grouped-bill__header">
      <div className="settle-grouped-bill__title">
        <Typography.Text strong>{group.orderNo}</Typography.Text>
        {group.orderDate && <span>{group.orderDate}</span>}
      </div>
      <Metric label="原纸" value={`${group.lines.length} 卷 / ${formatTon(group.originalWeight)}`} />
      <Metric label="成品" value={`${group.finishCount} 卷 / ${formatTon(group.finishWeight)}`} />
      <Metric label="切边" value={formatTon(group.trimWeight)} />
      <Metric label="加工费" value={formatMoney(group.processAmount)} />
      <Metric label="额外费" value={formatMoney(group.extraAmount)} hint={group.extraFeeSummary} />
      <strong>应收 {formatMoney(group.lineAmount)}</strong>
    </div>
  )
}

function Metric({ hint, label, value }: { hint?: string; label: string; value: string }) {
  return (
    <span>
      <em>{label}</em>
      {value}
      {hint && <small>{hint}</small>}
    </span>
  )
}

const groupLineColumns: ColumnsType<SettlePrintLine> = settlePrintLineColumns.filter(
  (column) => !isOrderNoColumn(column),
)

function isOrderNoColumn(column: ColumnGroupType<SettlePrintLine> | ColumnType<SettlePrintLine>) {
  return 'dataIndex' in column && column.dataIndex === 'orderNo'
}

import { Table, Tag, Typography } from 'antd'
import type { MouseEvent } from 'react'
import type { ColumnsType } from 'antd/es/table'
import type { TableRowSelection } from 'antd/es/table/interface'
import TooltipText from '../../components/biz/TooltipText'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventoryFinish } from '../../types/deliveryInventory'
import { formatSpecification } from './deliveryInventoryGrouping'
import './DeliveryInventoryFinishDetailTable.css'

interface Props {
  rows: DeliveryInventoryFinish[]
  selectedByUuid: Record<string, DeliveryInventoryFinish>
  onToggle: (row: DeliveryInventoryFinish, checked: boolean) => void
  selectionDisabled?: (row: DeliveryInventoryFinish) => boolean
}

export default function DeliveryInventoryFinishDetailTable({ rows, selectedByUuid, selectionDisabled, onToggle }: Props) {
  const rowSelection: TableRowSelection<DeliveryInventoryFinish> = {
    selectedRowKeys: rows.filter((row) => selectedByUuid[row.finishUuid]).map((row) => row.finishUuid),
    preserveSelectedRowKeys: true,
    getCheckboxProps: (record) => ({ disabled: record.stockState !== 1 || selectionDisabled?.(record) }),
    onChange: (keys) => syncSelection(rows, keys.map(String), onToggle),
  }

  return (
    <Table<DeliveryInventoryFinish>
      className="delivery-inventory-finish-detail-table"
      rowKey="finishUuid"
      size="small"
      columns={columns}
      dataSource={rows}
      pagination={false}
      rowSelection={rowSelection}
      onRow={(row) => detailRowProps(row, { onToggle, selectedByUuid, selectionDisabled })}
      bordered={false}
      tableLayout="fixed"
      scroll={{ x: 840 }}
      locale={{ emptyText: null }}
    />
  )
}

interface DetailRowInteraction {
  selectedByUuid: Record<string, DeliveryInventoryFinish>
  onToggle: Props['onToggle']
  selectionDisabled?: Props['selectionDisabled']
}

function detailRowProps(row: DeliveryInventoryFinish, interaction: DetailRowInteraction) {
  const disabled = row.stockState !== 1 || Boolean(interaction.selectionDisabled?.(row))
  const selected = Boolean(interaction.selectedByUuid[row.finishUuid])
  return {
    'aria-selected': selected,
    className: disabled ? 'delivery-inventory-detail-row--disabled' : 'delivery-inventory-detail-row--selectable',
    onClick: (event: MouseEvent<HTMLElement>) => {
      if (disabled || isInteractiveTarget(event.target)) return
      interaction.onToggle(row, !selected)
    },
  }
}

function isInteractiveTarget(target: EventTarget | null) {
  return target instanceof Element && Boolean(target.closest('a, button, input, label, [role="checkbox"]'))
}

function syncSelection(
  rows: DeliveryInventoryFinish[],
  selectedKeys: string[],
  onToggle: Props['onToggle'],
) {
  const selected = new Set(selectedKeys)
  rows.filter((row) => row.stockState === 1).forEach((row) => {
    onToggle(row, selected.has(row.finishUuid))
  })
}

const columns: ColumnsType<DeliveryInventoryFinish> = [
  {
    title: '成品卷号', dataIndex: 'finishRollNo', width: 140, ellipsis: true,
    render: (_value, row) => <Typography.Text strong><TooltipText value={row.finishRollNo} /></Typography.Text>,
  },
  {
    title: '品名 / 规格', dataIndex: 'paperName', width: 220,
    render: (_value, row) => (
      <div className="delivery-inventory-finish-spec">
        <TooltipText value={row.paperName} />
        <TooltipText value={formatSpecification(row)} />
      </div>
    ),
  },
  {
    title: '剩余重量', dataIndex: 'remainingWeight', width: 110, align: 'right',
    render: (_value, row) => <Typography.Text strong>{formatKg(row.remainingWeight)}</Typography.Text>,
  },
  {
    title: '类型', key: 'inventoryType', width: 90,
    render: (_value, row) => <Tag className={`delivery-inventory-tag delivery-inventory-tag--${typeTone(row)}`}>{typeText(row)}</Tag>,
  },
  {
    title: '状态', dataIndex: 'stockState', width: 90,
    render: (_value, row) => <Tag className={`delivery-inventory-tag delivery-inventory-tag--${row.stockState === 1 ? 'success' : 'warning'}`}>{row.stockState === 1 ? '可出库' : '已占用'}</Tag>,
  },
  {
    title: '待出库单', dataIndex: 'deliveryNo', width: 142, ellipsis: true,
    render: (_value, row) => <TooltipText value={row.deliveryNo || '-'} />,
  },
]

function typeText(row: DeliveryInventoryFinish) {
  if (row.isRemain === 1) return '余料'
  if (row.sourceType === 2) return '原纸直发'
  if (row.sourceType === 3) return '整理成品'
  return '成品'
}

function typeTone(row: DeliveryInventoryFinish) {
  if (row.isRemain === 1) return 'warning'
  if (row.sourceType === 2) return 'success'
  if (row.sourceType === 3) return 'primary'
  return 'primary'
}

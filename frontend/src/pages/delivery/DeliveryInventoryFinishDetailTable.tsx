import { Table, Tag, Typography } from 'antd'
import type { MouseEvent } from 'react'
import type { ColumnsType } from 'antd/es/table'
import type { TableRowSelection } from 'antd/es/table/interface'
import TooltipText from '../../components/biz/TooltipText'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventoryFinish } from '../../types/deliveryInventory'
import { formatSpecification } from './deliveryInventoryGrouping'
import './DeliveryInventoryFinishDetailTable.css'
import type { InventoryFinishSortController } from './useDeliveryInventoryCustomerSortState'
import {
  sortDeliveryInventoryFinishes,
  type DeliveryInventoryFinishSortField,
  type DeliveryInventoryFinishSortSpec,
} from './deliveryInventorySorting'

interface Props {
  rows: DeliveryInventoryFinish[]
  selectedByUuid: Record<string, DeliveryInventoryFinish>
  onToggle: (row: DeliveryInventoryFinish, checked: boolean) => void
  selectionDisabled?: (row: DeliveryInventoryFinish) => boolean
  sortState?: InventoryFinishSortController
}

export default function DeliveryInventoryFinishDetailTable(props: Props) {
  const { rows, selectedByUuid, selectionDisabled, onToggle } = props
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
      columns={buildColumns(props.sortState?.sortChain ?? [])}
      dataSource={props.sortState ? sortDeliveryInventoryFinishes(rows, props.sortState.sortChain) : rows}
      pagination={false}
      rowSelection={rowSelection}
      onChange={props.sortState?.onChange}
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

const baseColumns: ColumnsType<DeliveryInventoryFinish> = [
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

function buildColumns(sortChain: DeliveryInventoryFinishSortSpec[]): ColumnsType<DeliveryInventoryFinish> {
  return baseColumns.map((column) => {
    const field = detailSortField(column)
    if (!field) return column
    const active = sortChain.find((item) => item.field === field)
    const priority = sortChain.findIndex((item) => item.field === field)
    return {
      ...column,
      title: priority >= 0 && typeof column.title === 'string' ? `${column.title} ${priority + 1}` : column.title,
      sorter: { multiple: 1 },
      sortOrder: active?.direction === 'asc' ? 'ascend' : active?.direction === 'desc' ? 'descend' : null,
      sortDirections: ['ascend', 'descend', null],
    }
  })
}

function detailSortField(column: ColumnsType<DeliveryInventoryFinish>[number]): DeliveryInventoryFinishSortField | undefined {
  if (!('dataIndex' in column)) return undefined
  if (column.dataIndex === 'paperName') return 'specification'
  if (column.dataIndex === 'finishRollNo' || column.dataIndex === 'remainingWeight' || column.dataIndex === 'stockState' || column.dataIndex === 'deliveryNo') return column.dataIndex as DeliveryInventoryFinishSortField
  if (column.key === 'inventoryType') return 'inventoryType'
  return undefined
}

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

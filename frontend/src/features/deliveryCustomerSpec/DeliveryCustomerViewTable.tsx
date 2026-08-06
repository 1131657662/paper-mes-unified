import { Table, Tag, Typography } from 'antd'
import type { ColumnType, ColumnsType, TableProps } from 'antd/es/table'
import type { SortOrder } from 'antd/es/table/interface'
import type { DeliveryDetail } from '../../types/delivery'
import type { DeliveryCustomerSortField, DeliveryCustomerSortSpec } from '../../types/deliverySort'
import { formatKg, formatWholeKg } from '../../utils/numberFormatters'
import {
  sortDeliveryCustomerRows,
  type DeliveryCustomerTableRow,
} from './deliveryCustomerSorting'
import { resolveDeliveryCustomerRows } from './deliveryCustomerRows'
import type { DeliveryCustomerSpec, DeliveryDocumentView } from './deliveryCustomerSpecTypes'

interface Props {
  details: DeliveryDetail[]
  items?: DeliveryCustomerSpec[]
  view: Exclude<DeliveryDocumentView, 'physical'>
  sortChain: DeliveryCustomerSortSpec[]
  onChange: NonNullable<TableProps<DeliveryCustomerTableRow>['onChange']>
}

export default function DeliveryCustomerViewTable({ details, items = [], view, sortChain, onChange }: Props) {
  const rows = resolveDeliveryCustomerRows(details, items).rows
  const sortedRows = sortDeliveryCustomerRows(rows, sortChain)
  const total = rows.reduce((sum, row) => sum + (row.spec.customerDisplayWeight ?? 0), 0)
  return (
    <Table<DeliveryCustomerTableRow>
      bordered
      className="delivery-customer-table"
      columns={columns(view, sortChain)}
      dataSource={sortedRows}
      pagination={false}
      rowKey={({ spec }) => spec.deliveryDetailUuid}
      scroll={{ x: view === 'trace' ? 1160 : 900 }}
      size="small"
      onChange={onChange}
      summary={() => summary(sortedRows.length, total, view)}
    />
  )
}

type SortableColumn = {
  title: string
  field: DeliveryCustomerSortField
  width: number
  fixed?: 'left' | 'right'
  align?: 'left' | 'right' | 'center'
  render: ColumnType<DeliveryCustomerTableRow>['render']
}

function columns(
  view: Props['view'], sortChain: DeliveryCustomerSortSpec[],
): ColumnsType<DeliveryCustomerTableRow> {
  const result: ColumnsType<DeliveryCustomerTableRow> = [
    sortable({ title: '\u6210\u54c1\u5377\u53f7', field: 'finishRollNo', width: 150, fixed: 'left', render: (_, { spec }) => <Typography.Text strong>{spec.finishRollNo ?? '-'}</Typography.Text> }, sortChain),
    sortable({ title: '\u5ba2\u6237\u54c1\u540d', field: 'customerPaperName', width: 190, render: (_, { spec }) => <CustomerName spec={spec} trace={view === 'trace'} /> }, sortChain),
    sortable({ title: '\u5ba2\u6237\u89c4\u683c', field: 'customerSpecification', width: 190, render: (_, { spec }) => <CustomerSpecification spec={spec} trace={view === 'trace'} /> }, sortChain),
    sortable({ title: '\u5ba2\u6237\u5355\u636e\u91cd\u91cf', field: 'customerDisplayWeight', width: 165, align: 'right', render: (_, { spec }) => <Weight spec={spec} trace={view === 'trace'} /> }, sortChain),
    sortable({ title: '\u52a0\u5de5\u5355', field: 'orderNo', width: 150, render: (_, { spec }) => spec.orderNo ?? '-' }, sortChain),
    sortable({ title: '\u5ba2\u6237\u5907\u6ce8', field: 'customerRemark', width: 190, render: (_, { spec }) => spec.customerRemark || '-' }, sortChain),
  ]
  if (view === 'trace') {
    result.splice(5, 0, sortable({
      title: '\u6765\u6e90\u6bcd\u5377', field: 'sourceMotherRoll', width: 280,
      render: (_, { detail }) => detail?.originalSummary || detail?.originalRollNos || '-',
    }, sortChain))
  }
  result.unshift({ title: '\u5e8f\u53f7', key: 'rowNumber', width: 64, render: (_, __, index) => index + 1 })
  return result
}

function sortable(options: SortableColumn, sortChain: DeliveryCustomerSortSpec[]): ColumnType<DeliveryCustomerTableRow> {
  return {
    key: options.field,
    title: sortableTitle(options.title, options.field, sortChain),
    width: options.width,
    fixed: options.fixed,
    align: options.align,
    render: options.render,
    sorter: { multiple: 1 },
    sortOrder: sortOrder(options.field, sortChain),
    sortDirections: ['ascend', 'descend', null] as SortOrder[],
  }
}

function sortableTitle(title: string, field: DeliveryCustomerSortField, sortChain: DeliveryCustomerSortSpec[]) {
  const priority = sortChain.findIndex((item) => item.field === field)
  return <span>{title}{priority >= 0 ? <Typography.Text type="secondary"> {priority + 1}</Typography.Text> : null}</span>
}

function sortOrder(field: DeliveryCustomerSortField, sortChain: DeliveryCustomerSortSpec[]): SortOrder {
  const direction = sortChain.find((item) => item.field === field)?.direction
  return direction === 'asc' ? 'ascend' : direction === 'desc' ? 'descend' : null
}

function CustomerName({ spec, trace }: { spec: DeliveryCustomerSpec; trace: boolean }) {
  return <div className="delivery-customer-cell"><Typography.Text strong>{spec.customerPaperName ?? '-'}</Typography.Text>{trace && <span>实物：{spec.physicalPaperName ?? '-'}</span>}{spec.specificationChanged && <Tag color="gold">已调整</Tag>}</div>
}

function CustomerSpecification({ spec, trace }: { spec: DeliveryCustomerSpec; trace: boolean }) {
  const customer = `${spec.customerGramWeight ?? '-'}g / ${spec.customerFinishWidth ?? '-'}mm`
  const physical = `${spec.physicalGramWeight ?? '-'}g / ${spec.physicalFinishWidth ?? '-'}mm`
  return <div className="delivery-customer-cell"><Typography.Text>{customer}</Typography.Text>{trace && <span>实物：{physical}</span>}</div>
}

function Weight({ spec, trace }: { spec: DeliveryCustomerSpec; trace: boolean }) {
  return <div className="delivery-customer-cell is-right"><Typography.Text strong>{formatWholeKg(spec.customerDisplayWeight)}</Typography.Text>{trace && <span>实物：{formatKg(spec.physicalDeliveryWeight)}</span>}</div>
}

function summary(count: number, total: number, view: Props['view']) {
  const columns = view === 'trace' ? 8 : 7
  return <Table.Summary.Row className="delivery-customer-summary"><Table.Summary.Cell index={0}>客户单据合计</Table.Summary.Cell><Table.Summary.Cell index={1} colSpan={2}>{count} 件</Table.Summary.Cell><Table.Summary.Cell index={3} align="right">{formatWholeKg(total)}</Table.Summary.Cell><Table.Summary.Cell index={4} colSpan={columns - 4} /></Table.Summary.Row>
}

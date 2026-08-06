import { Button, Space, Tag, Typography } from 'antd'
import type { ColumnType, ColumnsType } from 'antd/es/table'
import type { SortOrder } from 'antd/es/table/interface'
import TooltipText from '../../components/biz/TooltipText'
import {
  deliveryDetailSpecText,
  deliveryOriginalSnapshotText,
  formatKg,
} from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryDetail } from '../../types/delivery'
import { formatGram } from '../../utils/numberFormatters'
import type { DeliveryDetailSortField, DeliverySortSpec } from './deliveryDetailSorting'

type SortableColumn = {
  title: string
  field: DeliveryDetailSortField
  dataIndex?: ColumnType<DeliveryDetail>['dataIndex']
  key?: string
  align?: 'left' | 'right' | 'center'
  width: number
  render?: ColumnType<DeliveryDetail>['render']
}

export function buildDeliveryDetailColumns(options: {
  canRemove?: boolean
  deliveryStatus?: number
  onRemove: (record: DeliveryDetail) => void
  sortChain?: DeliverySortSpec[]
}): ColumnsType<DeliveryDetail> {
  const sortChain = options.sortChain ?? []
  const columns: ColumnsType<DeliveryDetail> = [
    sortable({ title: '\u52a0\u5de5\u5355', field: 'orderNo', dataIndex: 'orderNo', width: 145, render: textCell }, sortChain),
    {
      ...sortableBase('finishRollNo', sortChain),
      title: sortableTitle('\u5377\u53f7', 'finishRollNo', sortChain),
      dataIndex: 'finishRollNo',
      fixed: 'left',
      width: 150,
      render: (value, record) => (
        <Space size={4} wrap>
          <Typography.Text strong>{value || '-'}</Typography.Text>
          {record.isRemain === 1 ? <Tag color="orange">余料</Tag> : null}
        </Space>
      ),
    },
    sortable({ title: '\u54c1\u540d', field: 'paperName', dataIndex: 'paperName', width: 130, render: textCell }, sortChain),
    sortable({ title: '\u514b\u91cd', field: 'gramWeight', dataIndex: 'gramWeight', width: 78, render: formatGram }, sortChain),
    sortable({ title: '\u89c4\u683c', field: 'spec', key: 'spec', width: 110, render: (_, record) => deliveryDetailSpecText(record) }, sortChain),
    sortable({ title: '\u4ef6\u91cd', field: 'actualWeight', dataIndex: 'actualWeight', align: 'right', width: 110, render: formatKg }, sortChain),
    sortable({ title: '\u51fa\u5e93\u91cd\u91cf', field: 'outWeight', dataIndex: 'outWeight', align: 'right', width: 110, render: formatKg }, sortChain),
    sortable({ title: '\u5269\u4f59\u53ef\u51fa\u5e93', field: 'remainingWeight', dataIndex: 'remainingWeight', align: 'right', width: 120, render: formatKg }, sortChain),
    {
      ...sortableBase('originalSummary', sortChain),
      title: sortableTitle('\u6765\u6e90\u6bcd\u5377', 'originalSummary', sortChain),
      dataIndex: 'originalSummary',
      width: 300,
      render: (_, record) => textCell(deliveryOriginalSnapshotText(record)),
    },
    sortable({ title: '\u5907\u6ce8', field: 'remark', dataIndex: 'remark', width: 150, render: textCell }, sortChain),
    sortable({ title: '\u56de\u5f55\u5907\u6ce8', field: 'actualRemark', dataIndex: 'actualRemark', width: 150, render: textCell }, sortChain),
  ]

  columns.unshift({ title: '\u5e8f\u53f7', key: 'rowNumber', width: 64, render: (_, __, index) => index + 1 })
  if (options.canRemove && options.deliveryStatus === 1) {
    columns.push({
      title: '\u64cd\u4f5c', key: 'actions', fixed: 'right', width: 86,
      render: (_, record) => (
        <Button type="link" size="small" danger onClick={() => options.onRemove(record)}>
          移出
        </Button>
      ),
    })
  }
  return columns
}

function sortable(options: SortableColumn, sortChain: DeliverySortSpec[]): ColumnType<DeliveryDetail> {
  return {
    ...sortableBase(options.field, sortChain),
    title: sortableTitle(options.title, options.field, sortChain),
    dataIndex: options.dataIndex,
    key: options.key,
    align: options.align,
    width: options.width,
    render: options.render,
  } as ColumnType<DeliveryDetail>
}

function sortableBase(field: DeliveryDetailSortField, sortChain: DeliverySortSpec[]) {
  return {
    sorter: { multiple: 1 },
    sortOrder: sortOrder(field, sortChain),
    sortDirections: ['ascend', 'descend', null] as SortOrder[],
  }
}

function sortableTitle(title: string, field: DeliveryDetailSortField, sortChain: DeliverySortSpec[]) {
  const priority = sortChain.findIndex((item) => item.field === field)
  return <Space size={4}>{title}{priority >= 0 ? <Typography.Text type="secondary">{priority + 1}</Typography.Text> : null}</Space>
}

function sortOrder(field: DeliveryDetailSortField, sortChain: DeliverySortSpec[]): SortOrder {
  const direction = sortChain.find((item) => item.field === field)?.direction
  return direction === 'asc' ? 'ascend' : direction === 'desc' ? 'descend' : null
}

function textCell(value?: string | number) {
  return <TooltipText value={value} />
}

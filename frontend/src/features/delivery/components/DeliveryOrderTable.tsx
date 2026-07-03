import { Button, Space, Tag, Typography } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import type { ReactNode } from 'react'
import type { TableRowSelection } from 'antd/es/table/interface'
import TooltipText from '../../../components/biz/TooltipText'
import { mesProTableOptions } from '../../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../../components/biz/tableToolbarPortalUtils'
import { useTableColumnsState } from '../../../hooks/useTableColumnsState'
import { useResizableTableColumns } from '../../../components/useResizableTableColumns'
import type { DeliveryOrder } from '../../../types/delivery'
import { DELIVERY_STATUS, SETTLE_BLOCK_ACTION } from '../../../constants/delivery'
import { formatTon } from '../utils/deliveryFormatters'

interface Props {
  canManageDelivery?: boolean
  data: DeliveryOrder[]
  loading: boolean
  onReload?: () => void
  rowClassName?: (record: DeliveryOrder) => string
  rowSelection?: TableRowSelection<DeliveryOrder>
  onConfirm: (record: DeliveryOrder) => void
  onDetail: (record: DeliveryOrder) => void
  onRow?: (record: DeliveryOrder) => React.HTMLAttributes<HTMLElement>
}

export default function DeliveryOrderTable({
  canManageDelivery = false,
  data,
  loading,
  onConfirm,
  onDetail,
  onReload,
  onRow,
  rowClassName,
  rowSelection,
}: Props) {
  const columns = buildColumns({ canManageDelivery, onConfirm, onDetail })
  const columnsState = useTableColumnsState('table-columns-delivery-orders')
  const resizable = useResizableTableColumns<DeliveryOrder, ProColumns<DeliveryOrder>>(columns, 'delivery-orders')

  return (
    <ProTable<DeliveryOrder>
      className="delivery-order-table mes-table-card"
      rowKey="uuid"
      loading={loading}
      columns={resizable.columns}
      columnsState={columnsState}
      components={resizable.components}
      dataSource={data}
      pagination={false}
      rowClassName={rowClassName}
      rowSelection={rowSelection}
      onRow={onRow}
      bordered
      cardProps={false}
      headerTitle={false}
      options={mesProTableOptions(onReload)}
      optionsRender={renderTableToolbarPortal}
      search={false}
      scroll={{ x: resizable.scrollX, y: '100%' }}
      tableAlertRender={false}
      tableAlertOptionRender={false}
      tableLayout="fixed"
      toolBarRender={() => []}
    />
  )
}

function buildColumns(actions: {
  canManageDelivery: boolean
  onConfirm: (record: DeliveryOrder) => void
  onDetail: (record: DeliveryOrder) => void
}): ProColumns<DeliveryOrder>[] {
  return [
    {
      title: '出库单号',
      dataIndex: 'deliveryNo',
      fixed: 'left',
      width: 150,
      render: (value) => <Typography.Text strong>{value}</Typography.Text>,
    },
    { title: '客户', dataIndex: 'customerName', width: 140, render: (_, record) => textCell(record.customerName) },
    { title: '日期', dataIndex: 'deliveryDate', width: 110 },
    {
      title: '出库统计',
      dataIndex: 'totalCount',
      width: 150,
      render: (_, record) => (
        <div className="delivery-cell-stack mes-cell-stack">
          <span>{record.totalCount} 卷</span>
          <span>{formatTon(record.totalWeight)}</span>
        </div>
      ),
    },
    { title: '提货人', dataIndex: 'pickerName', width: 110, render: (value) => value || '-' },
    { title: '车牌/柜号', dataIndex: 'carNo', width: 145, render: (_, record) => `${record.carNo || '-'} / ${record.containerNo || '-'}` },
    {
      title: '状态',
      dataIndex: 'deliveryStatus',
      width: 105,
      render: (_, record) => {
        const status = DELIVERY_STATUS[record.deliveryStatus]
        return status ? <Tag className="mes-status-tag" color={status.color}>{status.text}</Tag> : '-'
      },
    },
    {
      title: '结算拦截',
      dataIndex: 'settleBlockAction',
      width: 110,
      render: (_, record) => record.settleBlockAction
        ? <Tag className="mes-status-tag" color="orange">{SETTLE_BLOCK_ACTION[record.settleBlockAction]}</Tag>
        : '-',
    },
    {
      title: '操作',
      key: 'actions',
      className: 'delivery-order-table__actions-cell',
      fixed: 'right',
      width: 150,
      render: (_, record) => (
        <Space className="mes-action-buttons">
          <Button type="link" size="small" onClick={() => actions.onDetail(record)}>
            详情
          </Button>
          {actions.canManageDelivery && record.deliveryStatus === 1 && (
            <Button type="link" size="small" onClick={() => actions.onConfirm(record)}>
              签收
            </Button>
          )}
        </Space>
      ),
    },
  ]
}

function textCell(value?: ReactNode) {
  return <TooltipText value={value} />
}

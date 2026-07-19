import { Button, Space, Tag } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import type { DeliveryInventoryCustomer } from '../../types/deliveryInventory'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import TooltipText from '../../components/biz/TooltipText'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../components/biz/tableToolbarPortalUtils'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'

interface Props {
  canManage: boolean
  data: DeliveryInventoryCustomer[]
  fillHeight?: boolean
  loading: boolean
  onReload?: () => void
  onCreateDelivery: (customerUuid: string) => void
  onView: (customer: DeliveryInventoryCustomer) => void
  tableTitle?: string
}

export default function DeliveryInventoryCustomerTable(props: Props) {
  const resizable = useResizableTableColumns<DeliveryInventoryCustomer, ProColumns<DeliveryInventoryCustomer>>(
    buildColumns(props), 'delivery-inventory-customers',
  )
  const columnsState = useTableColumnsState('table-columns-delivery-inventory-customers')
  return (
    <ProTable<DeliveryInventoryCustomer>
      rowKey="customerUuid"
      headerTitle={props.tableTitle ?? '客户库存'}
      size="small"
      columns={resizable.columns}
      components={resizable.components}
      columnsState={columnsState}
      dataSource={props.data}
      loading={props.loading}
      pagination={false}
      scroll={props.fillHeight ? { x: resizable.scrollX, y: '100%' } : { x: resizable.scrollX }}
      options={mesProTableOptions(props.onReload)}
      optionsRender={renderTableToolbarPortal}
      cardProps={false}
      search={false}
      bordered
      tableLayout="fixed"
      toolBarRender={() => []}
    />
  )
}

function buildColumns(props: Props): ProColumns<DeliveryInventoryCustomer>[] {
  return [
    {
      title: '客户', dataIndex: 'customerName', fixed: 'left', width: 190, minWidth: 150, ellipsis: true,
      render: (value, row) => <Button type="link" className="delivery-inventory-customer-link" onClick={() => props.onView(row)}><TooltipText value={value} /></Button>,
    },
    { title: '实际在库', dataIndex: 'totalRollCount', align: 'right', width: 130, render: (_, row) => <WeightCell count={row.totalRollCount} weight={row.totalWeight} /> },
    { title: '可出库', dataIndex: 'availableRollCount', align: 'right', width: 130, render: (_, row) => <WeightCell count={row.availableRollCount} weight={row.availableWeight} tone="green" /> },
    { title: '锁定卷库存', dataIndex: 'lockedRollCount', align: 'right', width: 140, render: (_, row) => <WeightCell count={row.lockedRollCount} weight={row.lockedWeight} tone="orange" /> },
    { title: '计划出库', dataIndex: 'plannedOutWeight', align: 'right', width: 120, render: (_, row) => formatTon(row.plannedOutWeight) },
    { title: '仓库', dataIndex: 'warehouseNames', width: 150, ellipsis: true, render: (_, row) => <TooltipText value={row.warehouseNames} /> },
    { title: '主要品名', dataIndex: 'paperNames', width: 180, ellipsis: true, render: (_, row) => <TooltipText value={row.paperNames} /> },
    { title: '最早入库', dataIndex: 'oldestStockInTime', width: 150, ellipsis: true, render: (_, row) => <TooltipText value={formatDateTime(row.oldestStockInTime)} /> },
    {
      title: '操作', key: 'actions', className: 'delivery-inventory-customer-actions-cell', valueType: 'option', fixed: 'right', width: 168, minWidth: 168,
      render: (_, row) => <Space size={4} wrap><Button type="link" onClick={() => props.onView(row)}>库存明细</Button>{props.canManage && row.availableRollCount > 0 && <Button type="link" onClick={() => props.onCreateDelivery(row.customerUuid)}>新建出库</Button>}</Space>,
    },
  ]
}

function formatDateTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '待补录'
}

function WeightCell({ count, tone, weight }: { count: number; tone?: 'green' | 'orange'; weight: number }) {
  const tagTone = tone === 'green' ? 'success' : 'warning'
  return <div className="delivery-inventory-weight">{tone ? <Tag className={`delivery-inventory-tag delivery-inventory-tag--${tagTone}`}>{count} 卷</Tag> : <strong>{count} 卷</strong>}<span>{formatTon(weight)}</span></div>
}

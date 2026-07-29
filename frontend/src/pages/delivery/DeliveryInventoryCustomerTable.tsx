import { Button, Space, Tag } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import type { DeliveryInventoryCustomer } from '../../types/deliveryInventory'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventoryQuickFilter } from './deliveryInventoryModel'
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
  inventoryScope: DeliveryInventoryQuickFilter
  productInventoryLabel?: string
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
  const productLabel = props.productInventoryLabel ?? '成品（含直发）'
  const inventoryTitle = inventoryColumnTitle(props.inventoryScope, false, productLabel)
  const availableTitle = inventoryColumnTitle(props.inventoryScope, true, productLabel)
  return [
    {
      title: '客户', dataIndex: 'customerName', fixed: 'left', width: 190, minWidth: 150, ellipsis: true,
      render: (value, row) => <Button type="link" className="delivery-inventory-customer-link" onClick={() => props.onView(row)}><TooltipText value={value} /></Button>,
    },
    {
      title: inventoryTitle, dataIndex: 'productRollCount', width: 240, minWidth: 220,
      render: (_, row) => <InventoryBreakdownCell productLabel={productLabel} row={row} scope={props.inventoryScope} />,
    },
    {
      title: availableTitle, dataIndex: 'productAvailableRollCount', width: 240, minWidth: 220,
      render: (_, row) => <InventoryBreakdownCell available productLabel={productLabel} row={row} scope={props.inventoryScope} />,
    },
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

function inventoryColumnTitle(scope: DeliveryInventoryQuickFilter, available: boolean, productLabel: string) {
  if (scope === 'all') return available ? '可出库构成' : '库存构成'
  const label = scope === 'remain' ? '余料' : scope === 'direct' ? '原纸直发' : productLabel
  return available ? `可出库${label}` : `${label}库存`
}

interface InventoryMetric {
  count: number
  label: string
  showLabel: boolean
  tone: 'primary' | 'warning'
  weight: number
}

function InventoryBreakdownCell({ available = false, productLabel, row, scope }: { available?: boolean; productLabel: string; row: DeliveryInventoryCustomer; scope: DeliveryInventoryQuickFilter }) {
  const metrics = inventoryMetrics(row, available, scope, productLabel).filter(hasInventory)
  if (metrics.length === 0) return <span className="delivery-inventory-value--empty">—</span>
  return <div className="delivery-inventory-breakdown-cell">
    {metrics.map((metric) => <InventoryMetricLine key={metric.label} {...metric} />)}
  </div>
}

function inventoryMetrics(row: DeliveryInventoryCustomer, available: boolean, scope: DeliveryInventoryQuickFilter, productLabel: string): InventoryMetric[] {
  const showLabel = scope === 'all'
  const product = {
    count: available ? row.productAvailableRollCount : row.productRollCount,
    label: productLabel, showLabel, tone: 'primary' as const,
    weight: available ? row.productAvailableWeight : row.productWeight,
  }
  const remain = {
    count: available ? row.remainAvailableRollCount : row.remainRollCount,
    label: '余料', showLabel, tone: 'warning' as const,
    weight: available ? row.remainAvailableWeight : row.remainWeight,
  }
  const direct = {
    count: available ? row.directAvailableRollCount : row.directRollCount,
    label: '原纸直发', showLabel, tone: 'primary' as const,
    weight: available ? row.directAvailableWeight : row.directWeight,
  }
  if (scope === 'remain') return [remain]
  if (scope === 'direct') return [direct]
  if (scope === 'product') return [product]
  return [product, remain]
}

function hasInventory(metric: InventoryMetric) {
  return metric.count > 0 || metric.weight > 0
}

function InventoryMetricLine({ count, label, showLabel, tone, weight }: InventoryMetric) {
  const className = [
    'delivery-inventory-breakdown-cell__line',
    `delivery-inventory-breakdown-cell__line--${tone}`,
    showLabel ? '' : 'delivery-inventory-breakdown-cell__line--compact',
  ].filter(Boolean).join(' ')
  return <div className={className} aria-label={`${label} ${count} 卷 ${formatTon(weight)}`} title={showLabel ? undefined : label}>
    {showLabel && <span>{label}</span>}<strong>{count} 卷</strong><em>{formatTon(weight)}</em>
  </div>
}

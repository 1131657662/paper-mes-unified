import { Button, Tag, Typography } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import { formatKg } from '../../features/delivery/utils/deliveryFormatters'
import type { DeliveryInventoryFinish } from '../../types/deliveryInventory'
import { inventoryTypeText } from './deliveryInventoryModel'
import TooltipText from '../../components/biz/TooltipText'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../components/biz/tableToolbarPortalUtils'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'

export interface DeliveryInventoryTableSelection {
  selectedRowKeys: React.Key[]
  onChange: (keys: React.Key[], rows: DeliveryInventoryFinish[]) => void
  onToggle?: (row: DeliveryInventoryFinish, checked: boolean) => void
  disabled?: (row: DeliveryInventoryFinish) => boolean
}

interface Props {
  data: DeliveryInventoryFinish[]
  fillHeight?: boolean
  loading: boolean
  onReload?: () => void
  selection?: DeliveryInventoryTableSelection
  showCustomer?: boolean
  onOpenCustomer?: (customerUuid: string) => void
  onOpenDelivery: (uuid: string) => void
  tableTitle?: string
}

export default function DeliveryInventoryFinishTable(props: Props) {
  const resizable = useResizableTableColumns<DeliveryInventoryFinish, ProColumns<DeliveryInventoryFinish>>(
    finishColumns(props.showCustomer, props.onOpenCustomer, props.onOpenDelivery), 'delivery-inventory-finishes',
  )
  const columnsState = useTableColumnsState('table-columns-delivery-inventory-finishes')
  const rowSelection = props.selection ? {
    selectedRowKeys: props.selection.selectedRowKeys,
    preserveSelectedRowKeys: true,
    getCheckboxProps: (record: DeliveryInventoryFinish) => ({ disabled: record.stockState !== 1 || props.selection?.disabled?.(record) }),
    onChange: props.selection.onChange,
  } : undefined
  return (
    <ProTable<DeliveryInventoryFinish>
      rowKey="finishUuid"
      headerTitle={props.tableTitle ?? '成品卷库存'}
      size="small"
      columns={resizable.columns}
      components={resizable.components}
      columnsState={columnsState}
      dataSource={props.data}
      loading={props.loading}
      pagination={false}
      rowSelection={rowSelection}
      tableAlertRender={false}
      onRow={(row) => finishRowProps(row, props.selection)}
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

function finishRowProps(row: DeliveryInventoryFinish, selection?: DeliveryInventoryTableSelection) {
  if (!selection) return {}
  const disabled = Boolean(selection.disabled?.(row)) || row.stockState !== 1
  const selected = selection.selectedRowKeys.some((key) => String(key) === row.finishUuid)
  return {
    'aria-selected': selected,
    className: disabled ? 'delivery-inventory-finish-row--disabled' : 'delivery-inventory-finish-row--selectable',
    onClick: (event: React.MouseEvent<HTMLElement>) => {
      if (disabled || !selection.onToggle || isInteractiveTarget(event.target)) return
      selection.onToggle(row, !selected)
    },
  }
}

function isInteractiveTarget(target: EventTarget | null) {
  return target instanceof Element && Boolean(target.closest('a, button, input, label, [role="checkbox"]'))
}

function finishColumns(showCustomer = false, onOpenCustomer: ((uuid: string) => void) | undefined, onOpenDelivery: (uuid: string) => void): ProColumns<DeliveryInventoryFinish>[] {
  const columns: ProColumns<DeliveryInventoryFinish>[] = [
    { title: '成品卷号', dataIndex: 'finishRollNo', fixed: 'left', width: 135, minWidth: 120, ellipsis: true, render: (_, row) => <Typography.Text strong><TooltipText value={row.finishRollNo} /></Typography.Text> },
    { title: '加工单', dataIndex: 'orderNo', width: 160, minWidth: 140, ellipsis: true, render: (_, row) => <StackedCell main={row.orderNo} sub={row.orderDate} /> },
    { title: '品名/规格', dataIndex: 'paperName', width: 220, minWidth: 170, ellipsis: true, render: (_, row) => <StackedCell main={row.paperName} sub={specification(row)} /> },
    { title: '剩余重量', dataIndex: 'remainingWeight', align: 'right', width: 120, render: (_, row) => formatKg(row.remainingWeight) },
    { title: '仓库', dataIndex: 'warehouseName', width: 150, ellipsis: true, render: (_, row) => <StackedCell main={row.warehouseName || '未分配'} sub={row.warehouseLocation} /> },
    { title: '入库 / 库龄', dataIndex: 'stockInTime', width: 155, render: (_, row) => <StackedCell main={formatStockTime(row.stockInTime)} sub={row.stockAgeDays == null ? '库龄待补录' : `${row.stockAgeDays} 天`} /> },
    { title: '类型', key: 'inventoryType', width: 110, render: (_, row) => <Tag className={`delivery-inventory-tag delivery-inventory-tag--${inventoryTypeTone(row)}`}>{inventoryTypeText(row.isRemain, row.sourceType)}</Tag> },
    { title: '状态', dataIndex: 'stockState', width: 105, render: (_, row) => <StockStateTag value={row.stockState} /> },
    { title: '待出库单', dataIndex: 'deliveryNo', width: 160, ellipsis: true, render: (_, row) => <DeliveryLink label={row.deliveryNo} uuid={row.deliveryUuid} onOpen={onOpenDelivery} /> },
  ]
  if (showCustomer) columns.unshift({
    title: '客户', dataIndex: 'customerName', fixed: 'left', width: 180, minWidth: 140, ellipsis: true,
    render: (value, row) => onOpenCustomer
      ? <Button type="link" className="delivery-inventory-customer-link" onClick={() => onOpenCustomer(row.customerUuid)}><TooltipText value={value} /></Button>
      : <TooltipText value={value} />,
  })
  return columns
}

function formatStockTime(value?: string) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '待补录'
}

function StackedCell({ main, sub }: { main?: string; sub?: string }) {
  return <div className="delivery-cell-stack"><TooltipText value={main} /><TooltipText value={sub} /></div>
}

function specification(item: DeliveryInventoryFinish) {
  return [item.gramWeight && `${item.gramWeight}g`, item.finishWidth && `${item.finishWidth}mm`, item.finishDiameter && `Φ${item.finishDiameter}mm`].filter(Boolean).join(' / ') || '-'
}

function inventoryTypeTone(item: DeliveryInventoryFinish) {
  if (item.isRemain === 1) return 'warning'
  if (item.sourceType === 2) return 'success'
  return 'primary'
}

function StockStateTag({ value }: { value: 1 | 2 }) {
  return <Tag className={`delivery-inventory-tag delivery-inventory-tag--${value === 1 ? 'success' : 'warning'}`}>{value === 1 ? '可出库' : '已占用'}</Tag>
}

function DeliveryLink(props: { label?: string; onOpen: (uuid: string) => void; uuid?: string }) {
  if (!props.uuid) return '-'
  return <Button type="link" onClick={() => props.onOpen(props.uuid!)}><TooltipText value={props.label || props.uuid} /></Button>
}

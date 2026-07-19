import { Checkbox, Space } from 'antd'
import { ProTable } from '@ant-design/pro-components'
import type { ProColumns } from '@ant-design/pro-components'
import { useReducer, useState, type Key, type MouseEvent, type ReactNode, type TransitionEvent } from 'react'
import { formatTon } from '../../features/delivery/utils/deliveryFormatters'
import MesTooltip from '../../components/biz/MesTooltip'
import { mesProTableOptions } from '../../components/biz/mesProTableOptions'
import { renderTableToolbarPortal } from '../../components/biz/tableToolbarPortalUtils'
import type { DeliveryInventoryFinish, DeliveryInventoryOrderGroup } from '../../types/deliveryInventory'
import DeliveryInventoryFinishDetailTable from './DeliveryInventoryFinishDetailTable'

interface Props {
  groups: DeliveryInventoryOrderGroup[]
  loading: boolean
  selectedByUuid: Record<string, DeliveryInventoryFinish>
  selectionDisabled: (row: DeliveryInventoryFinish) => boolean
  onReload: () => void
  onToggle: (row: DeliveryInventoryFinish, checked: boolean) => void
  onToggleGroup: (group: DeliveryInventoryOrderGroup, checked: boolean) => void
}

export default function DeliveryInventoryOrderGroupTable(props: Props) {
  const [expansion, dispatchExpansion] = useReducer(expansionReducer, INITIAL_EXPANSION_STATE)
  const [hoveredRowKey, setHoveredRowKey] = useState<Key>()
  const visibleRenderedKeys = expansion.renderedKeys.filter((key) => props.groups.some((group) => group.orderUuid === key))
  const visibleExpandedKeys = visibleRenderedKeys.filter((key) => !expansion.closingKeys.has(key))
  const toggleExpanded = (group: DeliveryInventoryOrderGroup) => {
    dispatchExpansion({ type: 'toggle', key: group.orderUuid })
  }
  return <ProTable<DeliveryInventoryOrderGroup> className="delivery-inventory-customer-table delivery-inventory-order-group-table"
    tableClassName="delivery-inventory-order-group-grid"
    rowKey="orderUuid" headerTitle="加工单库存" size="small" dataSource={props.groups} loading={props.loading}
    columns={buildColumns(props, visibleExpandedKeys, hoveredRowKey)} options={mesProTableOptions(props.onReload)}
    optionsRender={renderTableToolbarPortal} cardProps={false} search={false} pagination={false} bordered tableLayout="fixed"
    onRow={(group) => groupRowProps(group, { expandedKeys: visibleExpandedKeys, toggleExpanded, setHoveredRowKey })}
    expandable={{ columnWidth: 48, expandedRowKeys: visibleRenderedKeys,
      onExpand: (_expanded, group) => toggleExpanded(group),
      rowExpandable: (group) => group.finishes.length > 0,
      expandedRowRender: (group) => <ExpandedDetailMotion closing={expansion.closingKeys.has(group.orderUuid)}
        onClosed={() => dispatchExpansion({ type: 'finish-close', key: group.orderUuid })}>
        <DeliveryInventoryFinishDetailTable rows={group.finishes} selectedByUuid={props.selectedByUuid}
          selectionDisabled={props.selectionDisabled} onToggle={props.onToggle} />
      </ExpandedDetailMotion> }}
    scroll={{ x: 798, y: '100%' }} toolBarRender={() => []} />
}

interface ExpansionState {
  renderedKeys: Key[]
  closingKeys: Set<Key>
}

type ExpansionAction = { type: 'toggle'; key: Key } | { type: 'finish-close'; key: Key }

const INITIAL_EXPANSION_STATE: ExpansionState = { renderedKeys: [], closingKeys: new Set() }

function expansionReducer(state: ExpansionState, action: ExpansionAction): ExpansionState {
  const closingKeys = new Set(state.closingKeys)
  if (action.type === 'finish-close') {
    if (!closingKeys.has(action.key)) return state
    return { renderedKeys: state.renderedKeys.filter((key) => key !== action.key), closingKeys }
  }
  if (closingKeys.delete(action.key)) {
    const renderedKeys = state.renderedKeys.includes(action.key) ? state.renderedKeys : [...state.renderedKeys, action.key]
    return { renderedKeys, closingKeys }
  }
  if (state.renderedKeys.includes(action.key)) {
    closingKeys.add(action.key)
    return { ...state, closingKeys }
  }
  return { renderedKeys: [...state.renderedKeys, action.key], closingKeys }
}

function ExpandedDetailMotion({ children, closing, onClosed }: { children: ReactNode; closing: boolean; onClosed: () => void }) {
  const stateClass = closing ? 'delivery-inventory-expanded-motion--closing' : 'delivery-inventory-expanded-motion--open'
  const finishClosing = (event: TransitionEvent<HTMLDivElement>) => {
    if (closing && event.target === event.currentTarget && event.propertyName === 'grid-template-rows') onClosed()
  }
  return <div className={`delivery-inventory-expanded-motion ${stateClass}`} aria-hidden={closing} onTransitionEnd={finishClosing}>
    <div className="delivery-inventory-expanded-motion__clip">
      <div className="delivery-inventory-expanded-motion__content">{children}</div>
    </div>
  </div>
}

function buildColumns(props: Props, expandedKeys: Key[], hoveredRowKey?: Key): ProColumns<DeliveryInventoryOrderGroup>[] {
  return [
    { title: '加工单', dataIndex: 'orderNo', width: 210, render: (_, group) => <Space><GroupCheckbox group={group} {...props} /><MesTooltip title={groupExpandHint(group, expandedKeys)} open={hoveredRowKey === group.orderUuid} placement="top"><span className="mes-tooltip-text">{group.orderNo}</span></MesTooltip></Space> },
    { title: '加工日期', dataIndex: 'orderDate', width: 120 },
    { title: '库存卷数', dataIndex: 'totalRollCount', align: 'right', width: 100, render: (_, row) => `${row.totalRollCount} 卷` },
    { title: '库存重量', dataIndex: 'totalWeight', align: 'right', width: 120, render: (_, row) => formatTon(row.totalWeight) },
    { title: '可出库', dataIndex: 'availableRollCount', align: 'right', width: 100, render: (_, row) => `${row.availableRollCount} 卷` },
    { title: '已占用', dataIndex: 'lockedRollCount', align: 'right', width: 100, render: (_, row) => `${row.lockedRollCount} 卷` },
  ]
}

function groupExpandHint(group: DeliveryInventoryOrderGroup, expandedKeys: Key[]) {
  if (!group.finishes.length) return `${group.orderNo} · 暂无库存明细`
  const action = expandedKeys.includes(group.orderUuid) ? '点击收起库存明细' : '点击展开库存明细'
  return `${group.orderNo} · ${action}`
}

interface GroupRowInteraction {
  expandedKeys: Key[]
  toggleExpanded: (group: DeliveryInventoryOrderGroup) => void
  setHoveredRowKey: (key?: Key) => void
}

function groupRowProps(group: DeliveryInventoryOrderGroup, interaction: GroupRowInteraction) {
  const expandable = group.finishes.length > 0
  return {
    'aria-expanded': expandable ? interaction.expandedKeys.includes(group.orderUuid) : undefined,
    className: expandable ? 'delivery-inventory-order-row--expandable' : '',
    onMouseEnter: () => interaction.setHoveredRowKey(group.orderUuid),
    onMouseLeave: () => interaction.setHoveredRowKey(),
    onClick: (event: MouseEvent<HTMLElement>) => {
      if (!expandable || isInteractiveTarget(event.target)) return
      interaction.toggleExpanded(group)
    },
  }
}

function isInteractiveTarget(target: EventTarget | null) {
  return target instanceof Element && Boolean(target.closest('a, button, input, label, [role="checkbox"]'))
}

function GroupCheckbox({ group, selectedByUuid, selectionDisabled, onToggleGroup }: Props & { group: DeliveryInventoryOrderGroup }) {
  const available = group.finishes.filter((row) => !selectionDisabled(row))
  const selected = available.filter((row) => selectedByUuid[row.finishUuid]).length
  return <Checkbox aria-label={`选择加工单 ${group.orderNo}`} disabled={!available.length} checked={available.length > 0 && selected === available.length}
    indeterminate={selected > 0 && selected < available.length} onChange={(event) => onToggleGroup(group, event.target.checked)} />
}

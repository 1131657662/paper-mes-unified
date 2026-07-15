import { useCallback, useMemo, useState } from 'react'
import { Table, Tooltip } from 'antd'
import type { ColumnType } from 'antd/es/table'
import { useResizableTableColumns } from '../../components/useResizableTableColumns'
import type { AvailableFinishVO } from '../../types/delivery'
import { createDeliverySelectionColumns } from './deliveryCreateColumns'
import {
  buildDeliveryOrderGroups,
  isDeliveryGroupRow,
  type DeliveryOrderGroupRow,
  type DeliverySelectionTableRow,
} from './deliveryFinishGrouping'
import { DELIVERY_SELECTION_TABLE_STORAGE_KEY } from './deliverySelectionTableConfig'
import type { DeliveryFinishScope } from './deliveryFinishScope'
import type { DeliveryLineEdit } from './deliverySelectionModel'

interface Props {
  data: AvailableFinishVO[]
  edits: Record<string, DeliveryLineEdit>
  emptyText: string
  loading: boolean
  scope: DeliveryFinishScope
  selectedRowKeys: React.Key[]
  onEditChange: (finishUuid: string, value: DeliveryLineEdit) => void
  onSelectionChange: (keys: React.Key[]) => void
}

export default function DeliveryCreateTable(props: Props) {
  const {
    data,
    edits,
    emptyText,
    loading,
    onEditChange,
    onSelectionChange,
    scope,
    selectedRowKeys,
  } = props
  const [collapsedGroupKeys, setCollapsedGroupKeys] = useState<Set<string>>(new Set())
  const selectGroup = useCallback((group: DeliveryOrderGroupRow) => {
    const next = new Set(selectedRowKeys)
    group.children.forEach((item) => next.add(item.finishUuid))
    onSelectionChange(Array.from(next))
  }, [onSelectionChange, selectedRowKeys])
  const clearGroup = useCallback((group: DeliveryOrderGroupRow) => {
    const groupKeys = new Set(group.children.map((item) => item.finishUuid))
    onSelectionChange(selectedRowKeys.filter((key) => !groupKeys.has(String(key))))
  }, [onSelectionChange, selectedRowKeys])
  const groups = useMemo(
    () => buildDeliveryOrderGroups(data, scope, selectedRowKeys, edits),
    [data, edits, scope, selectedRowKeys],
  )
  const columns = useMemo(
    () => createDeliverySelectionColumns({
      edits,
      onClearGroup: clearGroup,
      onEditChange,
      onSelectGroup: selectGroup,
      selectedRowKeys,
    }),
    [clearGroup, edits, onEditChange, selectGroup, selectedRowKeys],
  )
  const resizable = useResizableTableColumns<DeliverySelectionTableRow, ColumnType<DeliverySelectionTableRow>>(
    columns,
    DELIVERY_SELECTION_TABLE_STORAGE_KEY,
  )
  const expandedRowKeys = groups
    .map((group) => group.key)
    .filter((key) => !collapsedGroupKeys.has(key))
  const selectedKeys = new Set(selectedRowKeys.map(String))

  const handleExpand = (expanded: boolean, row: DeliverySelectionTableRow) => {
    if (!isDeliveryGroupRow(row)) return
    setCollapsedGroupKeys((current) => {
      const next = new Set(current)
      if (expanded) next.delete(row.key)
      else next.add(row.key)
      return next
    })
  }

  return (
    <Table<DeliverySelectionTableRow>
      className={`delivery-create-table${data.length === 0 ? ' delivery-create-table--empty' : ''}`}
      rowKey="key"
      size="small"
      loading={loading}
      columns={resizable.columns}
      components={resizable.components}
      dataSource={groups}
      locale={{ emptyText }}
      pagination={false}
      expandable={{
        childrenColumnName: 'children',
        expandRowByClick: true,
        expandedRowKeys,
        indentSize: 0,
        onExpand: handleExpand,
      }}
      rowSelection={{
        checkStrictly: false,
        columnWidth: 44,
        fixed: true,
        preserveSelectedRowKeys: true,
        selectedRowKeys,
        onChange: (keys) => onSelectionChange(removeGroupKeys(keys)),
        renderCell: (_, row, _index, originNode) => isDeliveryGroupRow(row)
          ? <Tooltip title={groupSelectionTitle(row)}><span>{originNode}</span></Tooltip>
          : originNode,
      }}
      rowClassName={(row) => rowClassName(row, selectedKeys)}
      onRow={(row) => isDeliveryGroupRow(row) ? {} : ({
        onClick: () => toggleKey(row.finishUuid, selectedRowKeys, onSelectionChange),
      })}
      scroll={data.length > 0
        ? { x: resizable.scrollX + 44, y: 'calc(100vh - 550px)' }
        : { y: 'calc(100vh - 550px)' }}
    />
  )
}

function groupSelectionTitle(group: DeliveryOrderGroupRow) {
  const scopeName = group.scope === 'remain' ? '余料' : '成品'
  return group.selectedCount === group.totalCount
    ? `清空本加工单${scopeName}选择`
    : `全选本加工单${scopeName}`
}

function removeGroupKeys(keys: React.Key[]) {
  return keys.filter((key) => !String(key).startsWith('delivery-order-group:'))
}

function rowClassName(row: DeliverySelectionTableRow, selectedKeys: Set<string>) {
  if (isDeliveryGroupRow(row)) return 'delivery-order-group-row'
  return selectedKeys.has(row.finishUuid) ? 'delivery-finish-row is-selected' : 'delivery-finish-row'
}

function toggleKey(
  key: string,
  selectedRowKeys: React.Key[],
  onSelectionChange: (keys: React.Key[]) => void,
) {
  if (selectedRowKeys.includes(key)) {
    onSelectionChange(selectedRowKeys.filter((item) => item !== key))
    return
  }
  onSelectionChange([...selectedRowKeys, key])
}

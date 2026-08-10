import { useCallback, useMemo, useState, type Key, type MouseEvent } from 'react'
import type { TableRowSelection } from 'antd/es/table/interface'
import type { ProcessOrder } from '../../types/processOrder'

const IGNORE_ROW_TOGGLE_SELECTOR = [
  'a',
  'button',
  'input',
  'textarea',
  'select',
  '[role="button"]',
  '.ant-checkbox-wrapper',
  '.ant-dropdown',
  '.ant-picker',
  '.ant-select',
  '.ant-input',
  '.ant-input-number',
  '.ant-table-selection-column',
  '.process-order-list__actions',
  '.process-order-resize-handle',
  '.resizable-col-handle',
].join(',')

export function useProcessOrderRowSelection() {
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [selectedRows, setSelectedRows] = useState<ProcessOrder[]>([])

  const clear = useCallback(() => {
    setSelectedRowKeys([])
    setSelectedRows([])
  }, [])

  const toggleRecord = useCallback((record: ProcessOrder) => {
    const selected = selectedRowKeys.includes(record.uuid)
    setSelectedRowKeys(selected ? [] : [record.uuid])
    setSelectedRows(selected ? [] : [record])
  }, [selectedRowKeys])

  const rowSelection = useMemo<TableRowSelection<ProcessOrder>>(() => ({
    type: 'radio',
    selectedRowKeys,
    columnWidth: 42,
    onChange: (keys, rows) => {
      setSelectedRowKeys(keys)
      setSelectedRows(rows)
    },
  }), [selectedRowKeys])

  const rowClassName = useCallback(
    (record: ProcessOrder) => selectedRowKeys.includes(record.uuid) ? 'process-order-list__row--selected' : '',
    [selectedRowKeys],
  )
  const onRow = useCallback((record: ProcessOrder) => ({
    onClick: (event: MouseEvent<HTMLElement>) => {
      if (!shouldToggleRow(event.target)) return
      toggleRecord(record)
    },
  }), [toggleRecord])

  return {
    clear,
    rowClassName,
    rowSelection,
    selectedRows,
    onRow,
  }
}

function shouldToggleRow(target: EventTarget | null) {
  if (!(target instanceof Element)) return true
  return !target.closest(IGNORE_ROW_TOGGLE_SELECTOR)
}

import { useState, type Key, type MouseEvent } from 'react'
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
].join(',')

export function useProcessOrderRowSelection() {
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [selectedRows, setSelectedRows] = useState<ProcessOrder[]>([])

  const clear = () => {
    setSelectedRowKeys([])
    setSelectedRows([])
  }

  const toggleRecord = (record: ProcessOrder) => {
    const selected = selectedRowKeys.includes(record.uuid)
    setSelectedRowKeys(selected ? selectedRowKeys.filter((key) => key !== record.uuid) : [...selectedRowKeys, record.uuid])
    setSelectedRows(selected ? selectedRows.filter((row) => row.uuid !== record.uuid) : [...selectedRows, record])
  }

  const rowSelection: TableRowSelection<ProcessOrder> = {
    selectedRowKeys,
    columnWidth: 42,
    onChange: (keys, rows) => {
      setSelectedRowKeys(keys)
      setSelectedRows(rows)
    },
  }

  return {
    clear,
    rowClassName: (record: ProcessOrder) => selectedRowKeys.includes(record.uuid) ? 'process-order-list__row--selected' : '',
    rowSelection,
    selectedRows,
    onRow: (record: ProcessOrder) => ({
      onClick: (event: MouseEvent<HTMLElement>) => {
        if (!shouldToggleRow(event.target)) return
        toggleRecord(record)
      },
    }),
  }
}

function shouldToggleRow(target: EventTarget | null) {
  if (!(target instanceof Element)) return true
  return !target.closest(IGNORE_ROW_TOGGLE_SELECTOR)
}

import { useState, type Key, type MouseEvent } from 'react'
import type { TableRowSelection } from 'antd/es/table/interface'

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
  '.resizable-col-handle',
  '.mes-action-buttons',
].join(',')

interface RowWithUuid {
  uuid: string
}

export function useDocumentRowSelection<RecordType extends RowWithUuid>() {
  const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])
  const [selectedRows, setSelectedRows] = useState<RecordType[]>([])

  const clear = () => {
    setSelectedRowKeys([])
    setSelectedRows([])
  }

  const toggleRecord = (record: RecordType) => {
    const selected = selectedRowKeys.includes(record.uuid)
    setSelectedRowKeys(selected ? selectedRowKeys.filter((key) => key !== record.uuid) : [...selectedRowKeys, record.uuid])
    setSelectedRows(selected ? selectedRows.filter((row) => row.uuid !== record.uuid) : [...selectedRows, record])
  }

  const rowSelection: TableRowSelection<RecordType> = {
    selectedRowKeys,
    columnWidth: 42,
    onChange: (keys, rows) => {
      setSelectedRowKeys(keys)
      setSelectedRows(rows)
    },
  }

  return {
    clear,
    rowClassName: (record: RecordType) => selectedRowKeys.includes(record.uuid) ? 'document-table-row--selected' : '',
    rowSelection,
    selectedRows,
    onRow: (record: RecordType) => ({
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

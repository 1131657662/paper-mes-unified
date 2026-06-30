import { ProTable } from '@ant-design/pro-components'
import type { ProColumns, ProTableProps } from '@ant-design/pro-components'
import type { ColumnType, ColumnsType, TableProps } from 'antd/es/table'
import { useMemo } from 'react'
import { useTableColumnsState } from '../../hooks/useTableColumnsState'
import { useResizableTableColumns } from '../useResizableTableColumns'

interface DocumentDetailTableProps<RecordType extends object>
  extends Omit<TableProps<RecordType>, 'bordered' | 'columns' | 'size'> {
  columns: ColumnsType<RecordType>
  defaultSize?: ProTableProps<RecordType, any>['defaultSize']
  onReload?: () => void
  storageKey: string
}

export default function DocumentDetailTable<RecordType extends object>({
  columns,
  defaultSize = 'small',
  onReload,
  storageKey,
  ...tableProps
}: DocumentDetailTableProps<RecordType>) {
  const columnsState = useTableColumnsState(`table-columns-${storageKey}`)
  const proColumns = useMemo(() => toProColumns(columns), [columns])
  const resizable = useResizableTableColumns<RecordType, ProColumns<RecordType>>(proColumns, storageKey)

  return (
    <div className="document-detail-table">
      <ProTable<RecordType>
        {...tableProps}
        bordered
        cardProps={false}
        className={['document-detail-table__grid', tableProps.className].filter(Boolean).join(' ')}
        columns={resizable.columns}
        columnsState={columnsState}
        components={resizable.components}
        defaultSize={defaultSize}
        headerTitle={false}
        options={{ density: true, reload: onReload ? () => onReload() : true, setting: true }}
        search={false}
        scroll={{ ...tableProps.scroll, x: tableProps.scroll?.x ?? resizable.scrollX }}
        tableAlertRender={false}
        tableAlertOptionRender={false}
        tableLayout="fixed"
        toolBarRender={() => []}
      />
    </div>
  )
}

function toProColumns<RecordType extends object>(columns: ColumnsType<RecordType>): ProColumns<RecordType>[] {
  return columns.map((column) => {
    if ('children' in column && column.children) {
      return {
        ...column,
        children: toProColumns(column.children),
      } as ProColumns<RecordType>
    }

    const leafColumn = column as ColumnType<RecordType>
    const render = leafColumn.render
    return {
      ...leafColumn,
      render: render
        ? (_dom, record, index) => render(recordValue(record, leafColumn.dataIndex), record, index)
        : undefined,
    } as ProColumns<RecordType>
  })
}

function recordValue<RecordType extends object>(
  record: RecordType,
  dataIndex: ColumnType<RecordType>['dataIndex'],
) {
  if (dataIndex == null) return undefined
  if (Array.isArray(dataIndex)) {
    return dataIndex.reduce<unknown>((value, key) => {
      if (value == null || typeof value !== 'object') return undefined
      return (value as Record<string, unknown>)[String(key)]
    }, record)
  }
  return (record as Record<string, unknown>)[String(dataIndex)]
}

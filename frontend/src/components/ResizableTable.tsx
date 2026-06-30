import { Table } from 'antd'
import type { ColumnType, TableProps } from 'antd/es/table'
import TooltipText from './biz/TooltipText'
import { resetResizableTableWidths, useResizableTableColumns } from './useResizableTableColumns'
import './ResizableTable.css'

interface ResizableTableProps<RecordType> extends Omit<TableProps<RecordType>, 'columns'> {
  storageKey: string
  columns: ColumnType<RecordType>[]
}

export { resetResizableTableWidths }

function wrapText<RecordType>(column: ColumnType<RecordType>): ColumnType<RecordType> {
  if (!column.ellipsis) return column
  const { ellipsis: _ellipsis, ...restColumn } = column
  const render = column.render
  return {
    ...restColumn,
    render: (value, record, index) => {
      const content = render ? render(value, record, index) : value
      if (content == null || content === '' || typeof content !== 'string') return content
      return <TooltipText value={content} />
    },
  }
}

export default function ResizableTable<RecordType extends object>({
  storageKey,
  columns,
  ...props
}: ResizableTableProps<RecordType>) {
  const wrappedColumns = columns.map(wrapText)
  const resizable = useResizableTableColumns<RecordType, ColumnType<RecordType>>(wrappedColumns, storageKey)

  return (
    <Table
      {...props}
      columns={resizable.columns}
      components={resizable.components}
      scroll={{ ...props.scroll, x: props.scroll?.x ?? resizable.scrollX }}
    />
  )
}

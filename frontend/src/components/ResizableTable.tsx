import { useMemo, useRef, useState } from 'react'
import type React from 'react'
import { Table, Typography } from 'antd'
import type { ColumnType, TableProps } from 'antd/es/table'
import { Resizable } from 'react-resizable'
import './ResizableTable.css'

type ColumnWidths = Record<string, number>

interface ResizableTableProps<RecordType> extends Omit<TableProps<RecordType>, 'columns'> {
  storageKey: string
  columns: ColumnType<RecordType>[]
}

interface ResizableTitleProps extends React.HTMLAttributes<HTMLTableCellElement> {
  width?: number
  onResize?: (event: React.SyntheticEvent, data: { size: { width: number } }) => void
}

const minWidth = 60
const maxWidth = 520

function columnKey<RecordType>(column: ColumnType<RecordType>) {
  return String(column.key ?? column.dataIndex ?? '')
}

function loadWidths(storageKey: string): ColumnWidths {
  const raw = localStorage.getItem(`table_cols_${storageKey}`)
  if (!raw) return {}
  try {
    return JSON.parse(raw) as ColumnWidths
  } catch {
    return {}
  }
}

function saveWidths(storageKey: string, widths: ColumnWidths) {
  localStorage.setItem(`table_cols_${storageKey}`, JSON.stringify(widths))
}

function ResizableTitle({ width, onResize, children, ...rest }: ResizableTitleProps) {
  if (!width) return <th {...rest}>{children}</th>
  return (
    <th {...rest} style={{ ...rest.style, position: 'relative' }}>
      <Resizable
        width={width}
        height={0}
        minConstraints={[minWidth, 0]}
        maxConstraints={[maxWidth, 0]}
        onResize={onResize}
        draggableOpts={{ enableUserSelectHack: false }}
        handle={(_, ref) => <span ref={ref} className="resizable-col-handle" />}
      >
        <div style={{ width, height: 0 }} />
      </Resizable>
      <div className="resizable-title-text">{children}</div>
    </th>
  )
}

function wrapText<RecordType>(column: ColumnType<RecordType>): ColumnType<RecordType> {
  if (!column.ellipsis) return column
  const render = column.render
  return {
    ...column,
    render: (value, record, index) => {
      const content = render ? render(value, record, index) : value
      if (content == null || content === '' || typeof content !== 'string') return content
      return <Typography.Text ellipsis={{ tooltip: content }}>{content}</Typography.Text>
    },
  }
}

export default function ResizableTable<RecordType extends object>({
  storageKey,
  columns,
  ...props
}: ResizableTableProps<RecordType>) {
  const saveTimer = useRef<ReturnType<typeof setTimeout>>()
  const [widths, setWidths] = useState<ColumnWidths>(() => loadWidths(storageKey))

  const mergedColumns = useMemo(() => {
    return columns.map((column) => {
      const key = columnKey(column)
      const width = widths[key] ?? column.width
      return wrapText({
        ...column,
        width,
        onHeaderCell: () => ({
          width,
          onResize: (_event: React.SyntheticEvent, data: { size: { width: number } }) => {
            const nextWidth = Math.max(minWidth, Math.min(maxWidth, data.size.width))
            setWidths((prev) => {
              const next = { ...prev, [key]: nextWidth }
              clearTimeout(saveTimer.current)
              saveTimer.current = setTimeout(() => saveWidths(storageKey, next), 250)
              return next
            })
          },
        }),
      } as ColumnType<RecordType>)
    })
  }, [columns, storageKey, widths])

  const scrollX = mergedColumns.reduce((sum, column) => sum + Number(column.width ?? 120), 0)

  return (
    <Table
      {...props}
      columns={mergedColumns}
      components={{ header: { cell: ResizableTitle } }}
      scroll={{ ...props.scroll, x: props.scroll?.x ?? scrollX }}
    />
  )
}

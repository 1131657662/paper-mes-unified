import { useEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import { Resizable } from 'react-resizable'
import type { ResizeCallbackData } from 'react-resizable'

type ColumnWidths = Record<string, number>

interface ResizableColumn<RecordType> {
  dataIndex?: unknown
  hideInTable?: boolean
  key?: React.Key
  onHeaderCell?: (record: RecordType, index?: number) => React.HTMLAttributes<HTMLElement>
  width?: number | string
}

interface ResizableHeaderCellProps extends React.ThHTMLAttributes<HTMLTableCellElement> {
  width?: number
  onResize?: (event: React.SyntheticEvent, data: ResizeCallbackData) => void
}

const minWidth = 60
const maxWidth = 520

export function useResizableTableColumns<
  RecordType,
  ColumnType,
>(columns: ColumnType[], storageKey: string) {
  const saveTimer = useRef<ReturnType<typeof setTimeout>>()
  const [widths, setWidths] = useState<ColumnWidths>(() => loadWidths(storageKey))

  useEffect(() => {
    const reset = (event: Event) => {
      const detail = (event as CustomEvent<{ storageKey?: string }>).detail
      if (detail?.storageKey === storageKey) setWidths({})
    }
    window.addEventListener('resizable-table-reset', reset)
    return () => window.removeEventListener('resizable-table-reset', reset)
  }, [storageKey])

  const resizedColumns = useMemo(() => {
    return columns.map((column) => {
      const resizableColumn = column as ResizableColumn<RecordType>
      const key = columnKey(resizableColumn)
      const width = key ? widths[key] ?? numericWidth(resizableColumn.width) : numericWidth(resizableColumn.width)
      if (!key || !width || resizableColumn.hideInTable) return column

      return {
        ...column,
        width,
        onHeaderCell: (record: RecordType, index?: number) => ({
          ...resizableColumn.onHeaderCell?.(record, index),
          width,
          onResize: (_event: React.SyntheticEvent, data: ResizeCallbackData) => {
            const nextWidth = Math.max(minWidth, Math.min(maxWidth, Math.round(data.size.width)))
            setWidths((prev) => {
              const next = { ...prev, [key]: nextWidth }
              clearTimeout(saveTimer.current)
              saveTimer.current = setTimeout(() => saveWidths(storageKey, next), 250)
              return next
            })
          },
        }),
      } as ColumnType
    })
  }, [columns, storageKey, widths])

  const scrollX = resizedColumns.reduce((sum, column) => {
    const resizableColumn = column as ResizableColumn<RecordType>
    return sum + (numericWidth(resizableColumn.width) ?? 120)
  }, 0)

  return {
    columns: resizedColumns,
    components: { header: { cell: ResizableHeaderCell } },
    scrollX,
  }
}

export function resetResizableTableWidths(storageKey: string) {
  localStorage.removeItem(widthStorageKey(storageKey))
  window.dispatchEvent(new CustomEvent('resizable-table-reset', { detail: { storageKey } }))
}

function ResizableHeaderCell({ onResize, width, ...restProps }: ResizableHeaderCellProps) {
  if (!width || !onResize) return <th {...restProps} />

  return (
    <Resizable
      width={width}
      height={0}
      minConstraints={[minWidth, 0]}
      maxConstraints={[maxWidth, 0]}
      handle={<span className="resizable-col-handle" onClick={(event) => event.stopPropagation()} />}
      onResize={onResize}
      draggableOpts={{ enableUserSelectHack: false }}
    >
      <th {...restProps} />
    </Resizable>
  )
}

function columnKey<RecordType>(column: ResizableColumn<RecordType>) {
  if (column.key != null) return String(column.key)
  if (Array.isArray(column.dataIndex)) return column.dataIndex.join('.')
  if (typeof column.dataIndex === 'string' || typeof column.dataIndex === 'number') return String(column.dataIndex)
  return undefined
}

function numericWidth(width: number | string | undefined) {
  return typeof width === 'number' ? width : undefined
}

function loadWidths(storageKey: string): ColumnWidths {
  const raw = localStorage.getItem(widthStorageKey(storageKey))
  if (!raw) return {}
  try {
    return JSON.parse(raw) as ColumnWidths
  } catch {
    return {}
  }
}

function saveWidths(storageKey: string, widths: ColumnWidths) {
  localStorage.setItem(widthStorageKey(storageKey), JSON.stringify(widths))
}

function widthStorageKey(storageKey: string) {
  return `table_cols_${storageKey}`
}

import { useEffect, useMemo, useRef, useState } from 'react'
import type React from 'react'
import type { ResizeCallbackData } from 'react-resizable'
import ResizableHeaderCell from './ResizableHeaderCell'
import {
  loadResizableTableWidths,
  resizableColumnMaxWidth,
  resizableColumnMinWidth,
  saveResizableTableWidths,
} from './resizableTableStorage'

interface ResizableColumn<RecordType> {
  dataIndex?: unknown
  hideInTable?: boolean
  key?: React.Key
  onHeaderCell?: (record: RecordType, index?: number) => React.HTMLAttributes<HTMLElement>
  valueType?: unknown
  width?: number | string
}

export function useResizableTableColumns<
  RecordType,
  ColumnType,
>(columns: ColumnType[], storageKey: string) {
  const saveTimer = useRef<ReturnType<typeof setTimeout>>()
  const [widths, setWidths] = useState(() => loadResizableTableWidths(storageKey))

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
      const width = resolvedWidth(resizableColumn, key, widths[key ?? ''])
      if (!key || !width || resizableColumn.hideInTable) return column
      if (isActionColumn(resizableColumn, key)) return { ...column, width } as ColumnType

      return {
        ...column,
        width,
        onHeaderCell: (record: RecordType, index?: number) => ({
          ...resizableColumn.onHeaderCell?.(record, index),
          width,
          onResize: (_event: React.SyntheticEvent, data: ResizeCallbackData) => {
            const nextWidth = Math.max(
              resizableColumnMinWidth,
              Math.min(resizableColumnMaxWidth, Math.round(data.size.width)),
            )
            setWidths((prev) => {
              const next = { ...prev, [key]: nextWidth }
              clearTimeout(saveTimer.current)
              saveTimer.current = setTimeout(() => saveResizableTableWidths(storageKey, next), 250)
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

function columnKey<RecordType>(column: ResizableColumn<RecordType>) {
  if (column.key != null) return String(column.key)
  if (Array.isArray(column.dataIndex)) return column.dataIndex.join('.')
  if (typeof column.dataIndex === 'string' || typeof column.dataIndex === 'number') return String(column.dataIndex)
  return undefined
}

function numericWidth(width: number | string | undefined) {
  return typeof width === 'number' ? width : undefined
}

function resolvedWidth<RecordType>(
  column: ResizableColumn<RecordType>,
  key: string | undefined,
  savedWidth: number | undefined,
) {
  const baseWidth = numericWidth(column.width)
  if (key && isActionColumn(column, key)) return baseWidth
  return savedWidth ?? baseWidth
}

function isActionColumn<RecordType>(column: ResizableColumn<RecordType>, key: string) {
  return key === 'actions' || key === 'operation' || column.valueType === 'option'
}

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
  minWidth?: number
  onHeaderCell?: (record: RecordType, index?: number) => React.HTMLAttributes<HTMLElement>
  valueType?: unknown
  width?: number | string
}

type ColumnWidths = Record<string, number>

export function useResizableTableColumns<
  RecordType,
  ColumnType,
>(columns: ColumnType[], storageKey: string) {
  const saveTimer = useRef<ReturnType<typeof setTimeout>>()
  const [widths, setWidths] = useState<ColumnWidths>(() => loadNormalizedWidths<RecordType, ColumnType>(columns, storageKey))

  useEffect(() => {
    const reset = (event: Event) => {
      const detail = (event as CustomEvent<{ storageKey?: string }>).detail
      if (detail?.storageKey === storageKey) setWidths({})
    }
    window.addEventListener('resizable-table-reset', reset)
    return () => window.removeEventListener('resizable-table-reset', reset)
  }, [storageKey])

  useEffect(() => {
    return () => clearTimeout(saveTimer.current)
  }, [])

  const resizedColumns = useMemo(() => {
    return columns.map((column) => {
      const resizableColumn = column as ResizableColumn<RecordType>
      const key = columnKey(resizableColumn)
      const minWidth = columnMinWidth(resizableColumn, key)
      const width = resolvedWidth(resizableColumn, key, widths[key ?? ''], minWidth)
      if (!key || !width || resizableColumn.hideInTable) return column
      if (isActionColumn(resizableColumn, key)) return { ...column, width } as ColumnType

      return {
        ...column,
        width,
        onHeaderCell: (record: RecordType, index?: number) => ({
          ...resizableColumn.onHeaderCell?.(record, index),
          minWidth,
          width,
          onResize: (_event: React.SyntheticEvent, data: ResizeCallbackData) => {
            const nextWidth = Math.max(
              minWidth,
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
    if (resizableColumn.hideInTable) return sum
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

function loadNormalizedWidths<RecordType, ColumnType>(columns: ColumnType[], storageKey: string): ColumnWidths {
  const savedWidths = loadResizableTableWidths(storageKey)
  return columns.reduce<ColumnWidths>((result, column) => {
    const resizableColumn = column as ResizableColumn<RecordType>
    const key = columnKey(resizableColumn)
    if (!key || savedWidths[key] == null) return result
    result[key] = clampWidth(savedWidths[key], columnMinWidth(resizableColumn, key))
    return result
  }, {})
}

function columnMinWidth<RecordType>(
  column: ResizableColumn<RecordType>,
  key: string | undefined,
) {
  const defaultWidth = key && isActionColumn(column, key) ? 168 : resizableColumnMinWidth
  const width = column.minWidth
  if (typeof width !== 'number' || !Number.isFinite(width)) return defaultWidth
  return Math.max(defaultWidth, Math.min(resizableColumnMaxWidth, Math.round(width)))
}

function resolvedWidth<RecordType>(
  column: ResizableColumn<RecordType>,
  key: string | undefined,
  savedWidth: number | undefined,
  minWidth: number,
) {
  const baseWidth = numericWidth(column.width)
  if (key && isActionColumn(column, key)) return baseWidth
  const width = savedWidth ?? baseWidth
  if (!width) return width
  return clampWidth(width, minWidth)
}

function isActionColumn<RecordType>(column: ResizableColumn<RecordType>, key: string) {
  return key === 'actions' || key === 'operation' || column.valueType === 'option'
}

function clampWidth(width: number, minWidth: number) {
  return Math.max(minWidth, Math.min(resizableColumnMaxWidth, Math.round(width)))
}

import { useState } from 'react'
import type { ResizeCallbackData } from 'react-resizable'
import type { ProColumns } from '@ant-design/pro-components'
import type { ProcessOrder } from '../../types/processOrder'
import ResizableHeaderCell from './ResizableHeaderCell'

type WidthMap = Record<string, number>

export function useResizableProcessColumns(columns: ProColumns<ProcessOrder>[]) {
  const [widthMap, setWidthMap] = useState<WidthMap>(() => readWidthMap())
  const resizedColumns = columns.map((column) => buildColumn(column, widthMap, setWidthMap))

  return {
    columns: resizedColumns,
    components: {
      header: {
        cell: ResizableHeaderCell,
      },
    },
  }
}

function buildColumn(
  column: ProColumns<ProcessOrder>,
  widthMap: WidthMap,
  setWidthMap: React.Dispatch<React.SetStateAction<WidthMap>>,
): ProColumns<ProcessOrder> {
  const key = columnKey(column)
  const width = key ? widthMap[key] ?? numericWidth(column.width) : numericWidth(column.width)
  if (!key || !width || column.hideInTable) return column

  return {
    ...column,
    width,
    onHeaderCell: () => ({
      width,
      onResize: (_event: React.SyntheticEvent, data: ResizeCallbackData) => {
        setWidthMap((prev) => saveWidthMap({ ...prev, [key]: Math.round(data.size.width) }))
      },
    }),
  }
}

function columnKey(column: ProColumns<ProcessOrder>) {
  if (column.key != null) return String(column.key)
  if (Array.isArray(column.dataIndex)) return column.dataIndex.join('.')
  return column.dataIndex == null ? undefined : String(column.dataIndex)
}

function numericWidth(width: ProColumns<ProcessOrder>['width']) {
  return typeof width === 'number' ? width : undefined
}

function readWidthMap(): WidthMap {
  try {
    const raw = localStorage.getItem(storageKey)
    return raw ? JSON.parse(raw) as WidthMap : {}
  } catch {
    return {}
  }
}

function saveWidthMap(next: WidthMap): WidthMap {
  try {
    localStorage.setItem(storageKey, JSON.stringify(next))
  } catch {
    // Ignore storage failures; resizing should still work in memory.
  }
  return next
}

const storageKey = 'table-column-widths-process-order'

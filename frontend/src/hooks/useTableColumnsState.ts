import { useState } from 'react'
import type { ColumnsState } from '@ant-design/pro-table/es/Store/Provide'

type ColumnsStateMap = Record<string, ColumnsState>

interface ColumnsStateConfig {
  value: ColumnsStateMap
  onChange: (value: ColumnsStateMap) => void
}

/**
 * ProTable 列配置持久化 Hook
 * 将用户自定义的列宽、显示/隐藏配置保存到 localStorage
 *
 * @param storageKey - localStorage 的 key，建议格式：'table-columns-{pageName}'
 * @returns columnsState 配置对象，可直接传给 ProTable 的 columnsState 属性
 */
export function useTableColumnsState(storageKey: string): ColumnsStateConfig {
  const [columnsState, setColumnsState] = useState<ColumnsStateConfig>(() => {
    const onChange = (value: ColumnsStateMap) => {
      setColumnsState((prev) => ({ ...prev, value }))
      try {
        localStorage.setItem(storageKey, JSON.stringify(value))
      } catch (error) {
        console.warn(`Failed to save table columns state for ${storageKey}:`, error)
      }
    }

    try {
      const saved = localStorage.getItem(storageKey)
      if (saved) {
        const parsed = parseColumnsStateMap(saved)
        return {
          value: parsed,
          onChange,
        }
      }
    } catch (error) {
      console.warn(`Failed to load table columns state for ${storageKey}:`, error)
    }

    return {
      value: {},
      onChange,
    }
  })

  return columnsState
}

function parseColumnsStateMap(value: string): ColumnsStateMap {
  const parsed: unknown = JSON.parse(value)
  if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
    return {}
  }
  return parsed as ColumnsStateMap
}

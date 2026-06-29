import { useState } from 'react'
import type { ProTableProps } from '@ant-design/pro-components'

/**
 * ProTable 列配置持久化 Hook
 * 将用户自定义的列宽、显示/隐藏配置保存到 localStorage
 *
 * @param storageKey - localStorage 的 key，建议格式：'table-columns-{pageName}'
 * @returns columnsState 配置对象，可直接传给 ProTable 的 columnsState 属性
 */
export function useTableColumnsState(storageKey: string) {
  const [columnsState, setColumnsState] = useState<
    ProTableProps<any, any>['columnsState']
  >(() => {
    const onChange = (value: any) => {
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
        const parsed = JSON.parse(saved)
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

import { useState } from 'react'
import { readTablePreferences, updateTablePreferences, type ColumnsStateMap } from './tablePreferences'

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
      updateTablePreferences(storageKey, (current) => ({ ...current, columns: value }))
    }
    return {
      value: readTablePreferences<never>(storageKey).columns,
      onChange,
    }
  })

  return columnsState
}

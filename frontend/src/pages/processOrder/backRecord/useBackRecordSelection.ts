import { useMemo, useState } from 'react'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { buildBackRecordWorkbench, workItemRecorded, workItemRollUuids } from './backRecordWorkbenchUtils'

interface SelectionState {
  scopeKey?: string
  keys: Set<string>
}

export function useBackRecordSelection(detail?: ProcessOrderDetailVO) {
  const [state, setState] = useState<SelectionState>({ keys: new Set() })
  const workbench = useMemo(() => detail ? buildBackRecordWorkbench(detail) : undefined, [detail])
  const remainingItems = workbench?.items.filter((item) => item.kind === 'roll' && !workItemRecorded(item)) ?? []
  const scopeKey = detail ? `${detail.order.uuid}:${detail.order.version ?? 0}` : undefined
  const defaultKeys = new Set(remainingItems.map((item) => item.key))
  const selectedKeys = state.scopeKey === scopeKey ? state.keys : defaultKeys
  const selectedItems = remainingItems.filter((item) => selectedKeys.has(item.key))
  const selectedRollUuids = new Set(selectedItems.flatMap(workItemRollUuids))
  const selectedFinishUuids = new Set(
    selectedItems.flatMap((item) => item.finishes.map(({ finish }) => finish.uuid)),
  )

  const toggle = (key: string, checked: boolean) => {
    const next = new Set(selectedKeys)
    if (checked) next.add(key)
    else next.delete(key)
    setState({ scopeKey, keys: next })
  }

  return {
    allRemainingSelected: remainingItems.length > 0 && selectedItems.length === remainingItems.length,
    remainingCount: remainingItems.length,
    selectedCount: selectedItems.length,
    selectedFinishUuids,
    selectedItemKeys: selectedKeys,
    selectedRollUuids,
    toggle,
  }
}

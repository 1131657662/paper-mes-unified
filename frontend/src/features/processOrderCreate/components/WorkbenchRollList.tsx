import { List } from 'antd'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import '../../../components/processOrder/ProcessOrderShared.css'
import { useWorkbenchRollSort } from '../hooks/useWorkbenchRollSort'
import { rollPreviewStatus } from '../previewStatusUtils'
import type { MergedSourceLock } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import WorkbenchRollItem, { type WorkbenchRollItemActions } from './WorkbenchRollItem'
import WorkbenchRollToolbar from './WorkbenchRollToolbar'

export interface WorkbenchRollListData {
  lockedRolls?: Record<string, MergedSourceLock>
  machines: Machine[]
  previews: Record<string, PlanPreviewVO>
  rolls: RollDraft[]
  routePreviews?: Record<string, ProcessRoutePreviewVO>
  serviceConfigured?: Record<string, boolean>
}

export interface WorkbenchRollListSelection {
  checkedIds: string[]
  selectedId?: string
}

export interface WorkbenchRollListActions extends WorkbenchRollItemActions {
  onClearSelection: () => void
  onSelectSameSpec: () => void
  selectAllLabel?: string
}

interface Props {
  actions: WorkbenchRollListActions
  data: WorkbenchRollListData
  selection: WorkbenchRollListSelection
}

export default function WorkbenchRollList({ actions, data, selection }: Props) {
  const { preference, sortedRolls, setPreference } = useWorkbenchRollSort(data.rolls)
  const lockedRolls = data.lockedRolls ?? {}
  const routePreviews = data.routePreviews ?? {}
  return (
    <div className="workbench-roll-list">
      <WorkbenchRollToolbar
        checkedCount={selection.checkedIds.length}
        preference={preference}
        onClearSelection={actions.onClearSelection}
        onSelectSameSpec={actions.onSelectSameSpec}
        selectAllLabel={actions.selectAllLabel}
        onSortChange={setPreference}
      />
      <List
        size="small"
        dataSource={sortedRolls}
        renderItem={(roll) => {
          const lock = lockedRolls[roll.localId]
          const originalIndex = data.rolls.findIndex((item) => item.localId === roll.localId)
          return (
            <WorkbenchRollItem
              actions={actions}
              state={{
                checked: selection.checkedIds.includes(roll.localId),
                index: originalIndex,
                lock,
                machines: data.machines,
                previewStatus: rollPreviewStatus({
                  roll,
                  preview: data.previews[roll.localId],
                  lock,
                  serviceConfigured: Boolean(roll.uuid && data.serviceConfigured?.[roll.uuid]),
                }),
                roll,
                routePreview: roll.uuid ? routePreviews[roll.uuid] : undefined,
                selected: selection.selectedId === roll.localId,
              }}
            />
          )
        }}
      />
    </div>
  )
}

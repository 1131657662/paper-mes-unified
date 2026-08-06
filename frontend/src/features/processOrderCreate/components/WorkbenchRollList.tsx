import { List } from 'antd'
import { useEffect, useRef } from 'react'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import '../../../components/processOrder/ProcessOrderShared.css'
import { useWorkbenchRollSort } from '../hooks/useWorkbenchRollSort'
import { rollPreviewStatus } from '../previewStatusUtils'
import { isConfiguredPlanReady, previewForRoll } from '../configuredPlanStatus'
import type { MergedSourceLock } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import type { ConfigOperation } from './configStepTypes'
import WorkbenchRollItem, { type WorkbenchRollItemActions } from './WorkbenchRollItem'
import WorkbenchRollToolbar from './WorkbenchRollToolbar'

export interface WorkbenchRollListData {
  configuredPlanIds?: string[]
  lockedRolls?: Record<string, MergedSourceLock>
  machines: Machine[]
  selectionDisabled?: boolean
  operation?: ConfigOperation
  plans?: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  rolls: RollDraft[]
  routePreviews?: Record<string, ProcessRoutePreviewVO>
  selectionDisabledReasons?: Record<string, string>
  serviceConfigured?: Record<string, boolean>
  serviceLoading?: boolean
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
  const listRef = useRef<HTMLDivElement>(null)
  const { preference, sortedRolls, setPreference } = useWorkbenchRollSort(data.rolls)
  const configured = new Set(data.configuredPlanIds ?? [])
  const lockedRolls = data.lockedRolls ?? {}
  const routePreviews = data.routePreviews ?? {}
  useEffect(() => {
    const selected = [...(listRef.current?.querySelectorAll<HTMLElement>('[data-roll-id]') ?? [])]
      .find((item) => item.dataset.rollId === selection.selectedId)
    selected?.scrollIntoView({ block: 'nearest' })
  }, [selection.selectedId])

  return (
    <div ref={listRef} className="workbench-roll-list">
      <WorkbenchRollToolbar
        checkedCount={selection.checkedIds.length}
        selectionDisabled={data.selectionDisabled}
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
                batchDisabledReason: data.selectionDisabledReasons?.[roll.localId],
                interactionDisabled: Boolean(data.selectionDisabled),
                index: originalIndex,
                lock,
                machines: data.machines,
                machineUuid: data.plans?.[roll.localId]?.machineUuid ?? roll.machineUuid,
                previewStatus: rollPreviewStatus({
                  configured: isConfiguredPlanReady(roll, configured, data.previews),
                  operation: selection.selectedId === roll.localId ? data.operation : undefined,
                  roll,
                  preview: previewForRoll(roll, data.previews),
                  lock,
                  serviceConfigured: serviceConfiguredForRoll(data.serviceConfigured, roll.uuid),
                  serviceLoading: data.serviceLoading,
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

function serviceConfiguredForRoll(
  configured: Record<string, boolean> | undefined,
  rollUuid: string | undefined,
): boolean | undefined {
  if (!configured || !rollUuid || !Object.hasOwn(configured, rollUuid)) return undefined
  return configured[rollUuid]
}

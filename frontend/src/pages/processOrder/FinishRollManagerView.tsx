import type { Key } from 'react'
import { Alert } from 'antd'
import type { ProcessOrderDetailVO } from '../../types/processOrder'
import FinishRollManagerTable from './FinishRollManagerTable'
import FinishRollManagerToolbar from './FinishRollManagerToolbar'
import FinishRollSummary from './FinishRollSummary'
import { filterFinishRolls, finishRollStats, type FinishRollFilters } from './finishRollManagerModel'

export interface FinishRollManagerState {
  filters: FinishRollFilters
  selectedKeys: Key[]
}

export interface FinishRollManagerActions {
  onAppendSpare: () => void
  onBatchVoid: () => void
  onChangeFilters: (filters: FinishRollFilters) => void
  onGenerate: () => void
  onSelectionChange: (keys: Key[]) => void
  onVoid: (uuid: string) => void
}

interface Props {
  actions: FinishRollManagerActions
  detail?: ProcessOrderDetailVO
  state: FinishRollManagerState
}

export default function FinishRollManagerView({ actions, detail, state }: Props) {
  const rolls = detail?.finishRolls ?? []
  const filteredRolls = filterFinishRolls(rolls, state.filters)
  const orderStatus = detail?.order.orderStatus ?? 0
  const readOnly = orderStatus !== 1
  const allDirectShip = detail?.originalRolls.every((roll) => roll.processMode === 3) ?? false
  return (
    <div className="finish-roll-manager">
      {readOnly && <Alert showIcon type="info" message="加工单下发后，成品号在此处只读；现场数量差异请在回录工作台调整实际产出。" />}
      <FinishRollSummary stats={finishRollStats(rolls)} />
      <FinishRollManagerToolbar
        actions={{ onAppendSpare: actions.onAppendSpare, onBatchVoid: actions.onBatchVoid, onChangeFilters: actions.onChangeFilters, onGenerate: actions.onGenerate }}
        state={{ allDirectShip, filteredCount: filteredRolls.length, filters: state.filters, readOnly, selectedCount: state.selectedKeys.length, totalCount: rolls.length }}
      />
      <FinishRollManagerTable rows={filteredRolls} selectedKeys={state.selectedKeys} readOnly={readOnly} onSelectionChange={actions.onSelectionChange} onVoid={actions.onVoid} />
    </div>
  )
}

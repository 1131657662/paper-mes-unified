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
  const settled = detail?.order.orderStatus === 5
  const allDirectShip = detail?.originalRolls.every((roll) => roll.processMode === 3) ?? false
  return (
    <div className="finish-roll-manager">
      {settled && <Alert showIcon type="warning" message="已结算订单费用已锁定，不可修改成品卷号" />}
      <FinishRollSummary stats={finishRollStats(rolls)} />
      <FinishRollManagerToolbar
        actions={{ onAppendSpare: actions.onAppendSpare, onBatchVoid: actions.onBatchVoid, onChangeFilters: actions.onChangeFilters, onGenerate: actions.onGenerate }}
        state={{ allDirectShip, filteredCount: filteredRolls.length, filters: state.filters, selectedCount: state.selectedKeys.length, settled, totalCount: rolls.length }}
      />
      <FinishRollManagerTable rows={filteredRolls} selectedKeys={state.selectedKeys} settled={settled} onSelectionChange={actions.onSelectionChange} onVoid={actions.onVoid} />
    </div>
  )
}

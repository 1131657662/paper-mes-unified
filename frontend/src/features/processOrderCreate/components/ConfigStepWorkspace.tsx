import { useState } from 'react'
import { Button, Card, Empty, Space, message } from 'antd'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type {
  PlanPreviewVO,
  ProcessPlanDTO,
  ProcessRoutePreviewVO,
  ProcessStep,
} from '../../../types/processOrder'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import type { DefaultPlanOptions } from '../draftMappers'
import { calculateRollWeightBalance } from '../weightBalanceModel'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import ConfigStepRoutePanel from './ConfigStepRoutePanel'
import PlanPreviewPanel from './PlanPreviewPanel'
import ProcessPlanEditor from './ProcessPlanEditor'
import ResizableWorkspace from './ResizableWorkspace'
import ServiceOnlyConfigEditor from './ServiceOnlyConfigEditor'
import ServiceOnlyPreviewPanel from './ServiceOnlyPreviewPanel'
import WorkbenchRollList from './WorkbenchRollList'

interface Data {
  allSteps: ProcessStep[]
  balance?: ReturnType<typeof calculateRollWeightBalance>
  checkedIds: string[]
  customerPrices?: CustomerProcessPrice[]
  defaultSpareCount: number
  detailError: boolean
  detailLoading: boolean
  lockedRolls: ReturnType<typeof mergedSourceLocks>
  machines: Machine[]
  orderUuid?: string
  plan?: ProcessPlanDTO
  planDefaults: DefaultPlanOptions
  previews: Record<string, PlanPreviewVO>
  roll?: RollDraft
  rolls: RollDraft[]
  routePreview?: ProcessRoutePreviewVO
  routePreviews: Record<string, ProcessRoutePreviewVO>
  saving: boolean
  selectedServiceRolls: RollDraft[]
  serviceConfigured: Record<string, boolean>
  serviceEditorStatus?: ServiceEditorStatus
  serviceOnly: boolean
}

interface Actions {
  onApplyChecked: () => Promise<void>
  onClearSelection: () => void
  onNext: () => void
  onOpenRouteDesigner: (roll: RollDraft) => void
  onPlanChange: (plan: ProcessPlanDTO) => void
  onPrev: () => void
  onPreviewCurrent: () => Promise<void>
  onRetryDetail: () => void
  onSaveCurrent: () => Promise<void>
  onSelect: (localId: string) => void
  onSelectSameSpec: () => void
  onServiceStatusChange: (status?: ServiceEditorStatus) => void
  onToggle: (localId: string, checked: boolean) => void
}

interface Props {
  actions: Actions
  data: Data
}

export default function ConfigStepWorkspace({ actions, data }: Props) {
  const [serviceActionHost, setServiceActionHost] = useState<HTMLDivElement | null>(null)
  const rollList = buildRollList(data, actions)
  const editor = buildEditor(data, actions, serviceActionHost)
  const preview = buildPreview(data, actions)

  return (
    <Card title="母卷加工方案工作台" className="process-config-workbench">
      <div className="process-config-workbench__workspace">
        <ResizableWorkspace
          leftTitle="母卷列表"
          mainTitle="工艺配置"
          rightTitle={data.serviceOnly ? '服务工艺状态' : '后端预览'}
          left={rollList}
          main={editor}
          right={preview}
          leftInitial={28}
          rightInitial={30}
        />
      </div>
      <div className="config-step-footer">
        <Space wrap className="config-step-footer__actions">
          <Button onClick={actions.onPrev}>上一步</Button>
          {!data.serviceOnly && <Button loading={data.saving} onClick={actions.onSaveCurrent}>保存当前</Button>}
          {!data.serviceOnly && <Button loading={data.saving} onClick={actions.onApplyChecked}>应用到选中</Button>}
          {data.roll?.processMode !== 3 && <div ref={setServiceActionHost} className="service-editor-footer" />}
          <Button type="primary" onClick={actions.onNext}>下一步：预览确认</Button>
        </Space>
      </div>
    </Card>
  )
}

function buildRollList(data: Data, actions: Actions) {
  return (
    <WorkbenchRollList
      data={{
        lockedRolls: data.lockedRolls,
        machines: data.machines,
        previews: data.previews,
        rolls: data.rolls,
        routePreviews: data.routePreviews,
        serviceConfigured: data.serviceConfigured,
      }}
      selection={{ checkedIds: data.checkedIds, selectedId: data.roll?.localId }}
      actions={{
        onClearSelection: actions.onClearSelection,
        onLockedSelect: (_, lock) => message.info(`该母卷已被 ${lock.ownerLabel} 合并使用，无需单独配置`),
        onOpenRouteDesigner: actions.onOpenRouteDesigner,
        onSelect: actions.onSelect,
        selectAllLabel: data.serviceOnly ? '全选仅附加工艺' : '全选同规格',
        onSelectSameSpec: actions.onSelectSameSpec,
        onToggle: actions.onToggle,
      }}
    />
  )
}

function buildEditor(data: Data, actions: Actions, footerContainer: HTMLElement | null) {
  const roll = data.roll
  const serviceEditor = roll && data.plan && roll.processMode !== 3
    ? buildServiceEditor(data, actions, footerContainer, roll)
    : null
  if (roll?.processMode === 4 && data.plan) {
    return serviceEditor
  }
  if (roll && data.routePreview) {
    return <div className="process-config-editor-stack">
      <ConfigStepRoutePanel preview={data.routePreview} roll={roll} onOpen={() => actions.onOpenRouteDesigner(roll)} />
      {serviceEditor}
    </div>
  }
  if (roll && data.plan) {
    return (
      <div className="process-config-editor-stack">
        <ProcessPlanEditor
          roll={roll}
          rolls={data.rolls}
          machines={data.machines}
          plan={data.plan}
          defaultSpareCount={data.defaultSpareCount}
          defaultPlanOptions={data.planDefaults}
          onChange={actions.onPlanChange}
        />
        {serviceEditor}
      </div>
    )
  }
  return <Empty description="请选择母卷" />
}

function buildServiceEditor(data: Data, actions: Actions, footerContainer: HTMLElement | null, roll: RollDraft) {
  return <ServiceOnlyConfigEditor
    customerPrices={data.customerPrices}
    detailError={data.detailError}
    detailLoading={data.detailLoading}
    allSteps={data.allSteps}
    orderUuid={data.orderUuid}
    roll={roll}
    selectedRolls={data.selectedServiceRolls}
    footerContainer={footerContainer}
    onStatusChange={actions.onServiceStatusChange}
    onRetryDetail={actions.onRetryDetail}
  />
}

function buildPreview(data: Data, actions: Actions) {
  if (data.serviceOnly) {
    return (
      <ServiceOnlyPreviewPanel
        loading={data.detailLoading}
        pending={data.serviceEditorStatus}
        roll={data.roll}
        steps={data.roll?.uuid ? serviceStepsForRoll(data.allSteps, data.roll.uuid) : []}
      />
    )
  }
  return (
    <PlanPreviewPanel
      balance={data.balance}
      preview={data.roll ? data.previews[data.roll.localId] : undefined}
      loading={data.saving}
      onPreview={actions.onPreviewCurrent}
    />
  )
}

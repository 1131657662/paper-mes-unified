import type { ReactNode } from 'react'
import { Card, Empty, message } from 'antd'
import type { ProcessPlanDTO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import { useConfigStepServiceCoordinator } from '../hooks/useConfigStepServiceCoordinator'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import { configStepProgress } from '../configStepProgress'
import { isConfiguredPlanReady } from '../configuredPlanStatus'
import { supportsSinglePlanEditing } from '../configStepSelection'
import type { RollDraft } from '../types'
import ConfigStepFooter from './ConfigStepFooter'
import ConfigStepRoutePanel from './ConfigStepRoutePanel'
import type {
  ConfigStepWorkspaceActions,
  ConfigStepWorkspaceData,
} from './configStepWorkspaceTypes'
import PlanPreviewPanel from './PlanPreviewPanel'
import ProcessPlanActions from './ProcessPlanActions'
import ProcessPlanEditor from './ProcessPlanEditor'
import ResizableWorkspace from './ResizableWorkspace'
import ServiceOnlyConfigEditor from './ServiceOnlyConfigEditor'
import ServiceOnlyPreviewPanel from './ServiceOnlyPreviewPanel'
import WorkbenchRollList from './WorkbenchRollList'

interface Props {
  actions: ConfigStepWorkspaceActions
  data: ConfigStepWorkspaceData
}

export default function ConfigStepWorkspace({ actions, data }: Props) {
  const service = useConfigStepServiceCoordinator({
    onDirtyChange: actions.onServiceDirtyChange,
    onSynchronizeVersion: actions.onSynchronizeVersion,
  })
  const progress = configStepProgress({
    configuredPlanIds: data.configuredPlanIds,
    lockedRolls: data.lockedRolls,
    previews: data.previews,
    routePreviews: data.routePreviews,
    rolls: data.rolls,
    serviceConfigured: data.serviceConfigured,
  })

  return (
    <Card title="母卷加工方案工作台" className="process-config-workbench">
      <div className="process-config-workbench__workspace">
        <ResizableWorkspace
          leftTitle="母卷列表"
          mainTitle="工艺配置"
          rightTitle={data.serviceOnly ? '服务工艺状态' : '后端预览'}
          left={buildRollList(data, actions, service.runAfterVersionSync)}
          main={buildEditor(data, actions, service)}
          right={buildPreview(data, service, actions)}
          leftInitial={28}
          rightInitial={30}
        />
      </div>
      <ConfigStepFooter
        hasUnsavedServiceChanges={service.status?.dirty === true || service.versionSyncBlocked}
        onNext={() => void service.runNext(actions.onNext)}
        onPrev={() => void service.runAfterVersionSync(actions.onPrev)}
        progress={progress}
        saving={data.saving}
        serviceWritePending={service.writePending}
      />
    </Card>
  )
}

function buildRollList(
  data: ConfigStepWorkspaceData,
  actions: ConfigStepWorkspaceActions,
  runAfterVersionSync: (action: () => void | Promise<void>) => Promise<void>,
) {
  return (
    <WorkbenchRollList
      data={{
        configuredPlanIds: data.configuredPlanIds,
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
        onOpenRouteDesigner: (roll) => void runAfterVersionSync(() => actions.onOpenRouteDesigner(roll)),
        onSelect: (localId) => void runAfterVersionSync(() => actions.onSelect(localId)),
        selectAllLabel: data.serviceOnly ? '全选仅附加工艺' : '全选同规格',
        onSelectSameSpec: actions.onSelectSameSpec,
        onToggle: actions.onToggle,
      }}
    />
  )
}

function buildEditor(
  data: ConfigStepWorkspaceData,
  actions: ConfigStepWorkspaceActions,
  service: ReturnType<typeof useConfigStepServiceCoordinator>,
) {
  const roll = data.roll
  const serviceEditor = roll && data.plan && roll.processMode !== 3
    ? buildServiceEditor(data, actions, service, roll)
    : null
  if (roll?.processMode === 4 && data.plan) return serviceEditor
  if (roll && data.routePreview) {
    return routeEditor(data.routePreview, actions, service, serviceEditor, roll)
  }
  if (roll && data.plan) {
    return singlePlanEditor(data, data.plan, actions, service, serviceEditor, roll)
  }
  return <Empty description="请选择母卷" />
}

function routeEditor(
  preview: ProcessRoutePreviewVO,
  actions: ConfigStepWorkspaceActions,
  service: ReturnType<typeof useConfigStepServiceCoordinator>,
  serviceEditor: ReactNode,
  roll: RollDraft,
) {
  return <div className="process-config-editor-stack">
    <ConfigStepRoutePanel preview={preview} roll={roll}
      onOpen={() => void service.runAfterVersionSync(() => actions.onOpenRouteDesigner(roll))} />
    {serviceEditor}
  </div>
}

function singlePlanEditor(
  data: ConfigStepWorkspaceData,
  plan: ProcessPlanDTO,
  actions: ConfigStepWorkspaceActions,
  service: ReturnType<typeof useConfigStepServiceCoordinator>,
  serviceEditor: ReactNode,
  roll: RollDraft,
) {
  return (
    <div className="process-config-editor-stack">
      <ProcessPlanEditor
        roll={roll} rolls={data.rolls} machines={data.machines} plan={plan}
        defaultSpareCount={data.defaultSpareCount} defaultPlanOptions={data.planDefaults}
        onChange={actions.onPlanChange}
      />
      {supportsSinglePlanEditing(roll.processMode) && (
        <ProcessPlanActions
          batchTargetCount={data.mainBatchTargetCount}
          checkedCount={data.checkedIds.length}
          onlyCurrentTarget={data.mainBatchOnlyCurrent}
          saved={isConfiguredPlanReady(roll, data.configuredPlanIds, data.previews)}
          saving={data.saving}
          onApply={() => void service.runAfterVersionSync(actions.onApplyChecked)}
          onSave={() => void service.runAfterVersionSync(actions.onSaveCurrent)}
        />
      )}
      {serviceEditor}
    </div>
  )
}

function buildServiceEditor(
  data: ConfigStepWorkspaceData,
  actions: ConfigStepWorkspaceActions,
  service: ReturnType<typeof useConfigStepServiceCoordinator>,
  roll: RollDraft,
) {
  return <ServiceOnlyConfigEditor
    customerPrices={data.customerPrices} detailError={data.detailError}
    detailLoading={data.detailLoading} allSteps={data.allSteps} orderUuid={data.orderUuid}
    roll={roll} selectedRolls={data.selectedServiceRolls} onStatusChange={service.changeStatus}
    onSynchronizeVersion={service.synchronizeVersion}
    onVersionSyncBlockedChange={service.changeVersionSyncBlocked}
    onWritePendingChange={service.changeWritePending}
    versionSyncBlocked={service.versionSyncBlocked}
    onRetryDetail={actions.onRetryDetail}
  />
}

function buildPreview(
  data: ConfigStepWorkspaceData,
  service: ReturnType<typeof useConfigStepServiceCoordinator>,
  actions: ConfigStepWorkspaceActions,
) {
  if (data.serviceOnly) {
    return <ServiceOnlyPreviewPanel loading={data.detailLoading} pending={service.status}
      roll={data.roll} steps={data.roll?.uuid ? serviceStepsForRoll(data.allSteps, data.roll.uuid) : []} />
  }
  return <PlanPreviewPanel balance={data.balance}
    configured={Boolean(data.roll
      && isConfiguredPlanReady(data.roll, data.configuredPlanIds, data.previews))}
    preview={data.roll ? data.previews[data.roll.localId] : undefined}
    loading={data.saving}
    onPreview={() => void service.runAfterVersionSync(actions.onPreviewCurrent)} />
}

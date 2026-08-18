import { Empty } from 'antd'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { isConfiguredPlanReady } from '../configuredPlanStatus'
import { supportsSinglePlanEditing } from '../configStepSelection'
import type { useConfigStepServiceCoordinator } from '../hooks/useConfigStepServiceCoordinator'
import type { RollDraft } from '../types'
import ConfigEditorTabs from './ConfigEditorTabs'
import ConfigStepRoutePanel from './ConfigStepRoutePanel'
import type { ConfigStepWorkspaceActions, ConfigStepWorkspaceData } from './configStepWorkspaceTypes'
import ProcessPlanActions from './ProcessPlanActions'
import ProcessPlanEditor from './ProcessPlanEditor'
import ServiceOnlyConfigEditor from './ServiceOnlyConfigEditor'
import AppendServiceStepEditor from './AppendServiceStepEditor'

interface Props {
  actions: ConfigStepWorkspaceActions
  data: ConfigStepWorkspaceData
  service: ReturnType<typeof useConfigStepServiceCoordinator>
}

export default function ConfigStepEditorPanel({ actions, data, service }: Props) {
  const serviceEditor = data.roll && data.plan && data.roll.processMode !== 3
    ? buildServiceEditor({ actions, data, roll: data.roll, service })
    : null
  const editor = data.serviceOnly ? serviceEditor : (
    <ConfigEditorTabs
      active={data.activeEditor}
      main={buildMainEditor({ actions, data, service })}
      onChange={actions.onEditorChange}
      service={serviceEditor ?? <Empty description="当前母卷不支持附加工艺" />}
    />
  )
  return <fieldset
    aria-busy={data.operation === 'saving'}
    className="process-config-editor-lock"
    disabled={data.operation === 'saving'}
  >{editor}</fieldset>
}

function buildMainEditor({ actions, data, service }: Props) {
  const roll = data.roll
  if (roll && data.routePreview) {
    return <ConfigStepRoutePanel preview={data.routePreview} roll={roll}
      onOpen={() => void service.runAfterVersionSync(() => actions.onOpenRouteDesigner(roll))} />
  }
  if (roll && data.plan) return singlePlanEditor({ actions, data, plan: data.plan, roll, service })
  return <Empty description="请选择母卷" />
}

function singlePlanEditor(options: Props & { plan: ProcessPlanDTO; roll: RollDraft }) {
  const { actions, data, plan, roll, service } = options
  return <div className="process-config-editor-stack">
    <ProcessPlanEditor roll={roll} rolls={data.rolls} machines={data.machines} plan={plan}
      onEditMode={() => void service.runNext(actions.onPrev)} onChange={actions.onPlanChange} />
    {supportsSinglePlanEditing(roll.processMode) && <ProcessPlanActions
      batchTargetCount={data.mainBatchTargetCount}
      previewReady={data.previews[roll.localId]?.ready === true}
      saved={isConfiguredPlanReady(roll, data.configuredPlanIds, data.previews)}
      saving={data.operation === 'saving'}
      onExecute={() => void service.runAfterVersionSync(actions.onApplyChecked)} />}
  </div>
}

function buildServiceEditor(options: Props & { roll: RollDraft }) {
  const { actions, data, roll, service } = options
  if (data.onServiceStepsChange) {
    return <AppendServiceStepEditor
      allSteps={data.allSteps}
      customerPrices={data.customerPrices}
      roll={roll}
      rolls={data.rolls}
      selectedRolls={data.selectedServiceRolls}
      onPersist={data.onServiceStepsChange}
      onBatchApplied={actions.onServiceBatchApplied}
      onCurrentSaved={actions.onCurrentServiceSaved}
      onStatusChange={service.changeStatus}
    />
  }
  return <ServiceOnlyConfigEditor
    aiPackagingDraft={data.aiPackagingDraft}
    customerPrices={data.customerPrices} detailError={data.detailError}
    detailLoading={data.detailLoading} allSteps={data.allSteps} draftVersion={data.draftVersion}
    orderUuid={data.orderUuid}
    roll={roll} selectedRolls={data.selectedServiceRolls} onStatusChange={service.changeStatus}
    onAiPackagingDraftConsumed={actions.onAiPackagingDraftConsumed}
    onAiPackagingDraftDismissed={actions.onAiPackagingDraftDismissed}
    onBatchApplied={actions.onServiceBatchApplied}
    onCurrentSaved={actions.onCurrentServiceSaved}
    onSynchronizeVersion={service.synchronizeLatest}
    onVersionSyncBlockedChange={service.changeVersionSyncBlocked}
    onWritePendingChange={service.changeWritePending}
    versionSyncBlocked={service.versionSyncBlocked}
    onRetryDetail={actions.onRetryDetail}
  />
}

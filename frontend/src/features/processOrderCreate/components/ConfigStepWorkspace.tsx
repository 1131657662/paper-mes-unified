import { Card, message } from 'antd'
import { useConfigStepServiceCoordinator } from '../hooks/useConfigStepServiceCoordinator'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import { configStepProgress } from '../configStepProgress'
import { isConfiguredPlanReady } from '../configuredPlanStatus'
import ConfigStepEditorPanel from './ConfigStepEditorPanel'
import ConfigStepFooter from './ConfigStepFooter'
import type {
  ConfigStepWorkspaceActions,
  ConfigStepWorkspaceData,
} from './configStepWorkspaceTypes'
import PlanPreviewPanel from './PlanPreviewPanel'
import ResizableWorkspace from './ResizableWorkspace'
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
    onWritePendingChange: actions.onServicePendingChange,
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
    <Card title="母卷加工方案工作台" extra={data.assistantEntry} className="process-config-workbench">
      <div className="process-config-workbench__workspace">
        <ResizableWorkspace
          leftTitle="母卷列表"
          mainTitle="工艺配置"
          rightTitle={data.serviceOnly ? '服务工艺状态' : '后端预览'}
          left={buildRollList(data, actions, service.runSelection)}
          main={<ConfigStepEditorPanel actions={actions} data={data} service={service} />}
          right={buildPreview(data, service, actions)}
          leftInitial={25}
          rightInitial={26}
        />
      </div>
      <ConfigStepFooter
        autoFinishConfigEnabled={data.autoFinishConfigEnabled}
        configurationLoading={data.detailLoading || data.aiPackagingLoading}
        hasUnsavedServiceChanges={service.status?.dirty === true || service.versionSyncBlocked
          || data.aiPackagingDraftCount > 0}
        pendingAiPackagingCount={data.aiPackagingDraftCount}
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
  runSelection: (action: () => void | Promise<void>) => Promise<void>,
) {
  return (
    <WorkbenchRollList
      data={{
        configuredPlanIds: data.configuredPlanIds,
        lockedRolls: data.lockedRolls,
        machines: data.machines,
        plans: data.plans,
        operation: data.operation,
        previews: data.previews,
        rolls: data.rolls,
        selectionDisabledReasons: data.selectionDisabledReasons,
        selectionDisabled: data.operation === 'saving',
        routePreviews: data.routePreviews,
        serviceConfigured: data.serviceConfigured,
        serviceLoading: data.detailLoading,
      }}
      selection={{ checkedIds: data.checkedIds, selectedId: data.roll?.localId }}
      actions={{
        onClearSelection: actions.onClearSelection,
        onLockedSelect: (_, lock) => message.info(`该母卷已被 ${lock.ownerLabel} 合并使用，无需单独配置`),
        onOpenRouteDesigner: (roll) => void runSelection(() => actions.onOpenRouteDesigner(roll)),
        onSelect: (localId) => void runSelection(() => actions.onSelect(localId)),
        selectAllLabel: selectionLabel(data),
        onSelectSameSpec: actions.onSelectSameSpec,
        onToggle: actions.onToggle,
      }}
    />
  )
}

function selectionLabel(data: ConfigStepWorkspaceData) {
  if (data.serviceOnly) return '全选仅附加工艺'
  return data.activeEditor === 'service' ? '全选可用卷' : '全选同规格'
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
    disabled={data.operation === 'saving'}
    configured={Boolean(data.roll
      && isConfiguredPlanReady(data.roll, data.configuredPlanIds, data.previews))}
    preview={data.roll ? data.previews[data.roll.localId] : undefined}
    error={data.previewError}
    loading={data.previewing}
    onRetry={() => void service.runAfterVersionSync(actions.onPreviewCurrent)}
    onPreview={() => void service.runAfterVersionSync(actions.onPreviewCurrent)} />
}

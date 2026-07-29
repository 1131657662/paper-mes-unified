import { message } from 'antd'
import type { ConfigEditorTab } from './components/configStepWorkspaceTypes'
import type { ConfigStepProps } from './components/configStepTypes'
import { nextPendingConfigRoll } from './configStepProgress'
import { sameSpecRollIds } from './configStepSelection'
import { isConfiguredPlanReady } from './configuredPlanStatus'
import type { useConfigStepBatchSelections } from './hooks/useConfigStepBatchSelections'
import type { ConfigStepWorkspaceModel } from './hooks/useConfigStepWorkspaceModel'
import { freshDraftVersion } from './serviceVersionSync'

export interface ConfigStepCommandContext {
  model: ConfigStepWorkspaceModel
  props: ConfigStepProps
  selections: ReturnType<typeof useConfigStepBatchSelections>
  setPreferredEditor: (editor: ConfigEditorTab) => void
}

export function toggleRoll(context: ConfigStepCommandContext, localId: string, checked: boolean) {
  const { model, selections } = context
  if (model.data.operation === 'saving' || model.data.selectionDisabledReasons[localId]) return
  const selection = model.data.activeEditor === 'service' ? selections.service : selections.plan
  selection.toggle(localId, checked)
}

export function selectSameSpec(context: ConfigStepCommandContext) {
  const { model, selections } = context
  if (model.data.operation === 'saving' || !model.selected) return
  if (model.data.activeEditor === 'plan') {
    selections.plan.replace(sameSpecRollIds({
      locks: model.data.lockedRolls,
      rolls: model.data.rolls,
      selected: model.selected,
    }))
    return
  }
  selections.service.replace(model.configurableRolls
    .filter((roll) => roll.uuid && roll.localId !== model.selected!.localId)
    .filter((roll) => model.data.serviceOnly ? roll.processMode === 4 : roll.processMode !== 3)
    .map((roll) => roll.localId))
}

function readyForPlanCommand(context: ConfigStepCommandContext): boolean {
  const { model, props } = context
  if (props.orderUuid && model.selected?.uuid && model.data.plan) return true
  message.warning('请先保存原纸明细')
  return false
}

export async function saveCurrent(context: ConfigStepCommandContext) {
  const { model, props } = context
  if (!readyForPlanCommand(context) || !model.selected || !model.data.plan) return
  const result = await props.onSavePlan(model.selected, model.data.plan)
  if (result && result.applied && result.preview.ready) {
    selectNextPending(context, [model.selected.localId])
  }
}

export async function previewCurrent(context: ConfigStepCommandContext) {
  const { model } = context
  if (model.data.operation === 'saving') return
  if (!readyForPlanCommand(context) || !model.selected || !model.data.plan) return
  await model.previewCurrent()
}

export async function synchronizeVersion(context: ConfigStepCommandContext) {
  const { model, props } = context
  const result = await model.detailQuery.refetch({ cancelRefetch: false })
  const version = freshDraftVersion(result.data, props.draftVersion)
  if (version != null) {
    props.onDraftVersionChange(version)
    return version
  }
  message.error('附加工艺已提交，但草稿版本刷新失败；请重试同步后再继续')
  throw new Error('draft version synchronization failed')
}

export async function applyToChecked(context: ConfigStepCommandContext) {
  const { model, props, selections, setPreferredEditor } = context
  if (!model.data.plan) return
  const current = model.selected
  const currentDirty = Boolean(current
    && !isConfiguredPlanReady(current, model.data.configuredPlanIds, model.data.previews))
  if (!model.batchTargets.length) {
    if (currentDirty) {
      await saveCurrent(context)
      return
    }
    message.info('当前方案已保存；选择兼容母卷后可批量应用')
    return
  }
  const targets = currentDirty && current ? [current, ...model.batchTargets] : model.batchTargets
  const result = await props.onSavePlanBatch(targets, model.data.plan)
  if (!result) return
  const targetFailures = result.failedIds.filter((id) => id !== current?.localId)
  selections.plan.replace(targetFailures)
  if (current && result.failedIds.includes(current.localId)) {
    setPreferredEditor('plan')
    props.onSelect(current.localId)
    return
  }
  if (!result.failedIds[0]) {
    selectNextPending(context, result.savedIds)
    return
  }
  setPreferredEditor('plan')
  props.onSelect(targetFailures[0] ?? result.failedIds[0])
}

export function selectRoll(context: ConfigStepCommandContext, localId: string) {
  if (context.model.data.operation === 'saving') return
  const lock = context.model.data.lockedRolls[localId]
  if (!lock) {
    if (context.props.selectedId !== localId) clearBatchSelections(context)
    context.props.onSelect(localId)
    return
  }
  message.info(`该母卷已被 ${lock.ownerLabel} 合并使用，无需单独配置`)
}

export function runUnlessSaving(context: ConfigStepCommandContext, action: () => void) {
  if (context.model.data.operation !== 'saving') action()
}

export function continueToNextStep(context: ConfigStepCommandContext) {
  const { model, props } = context
  if (model.detailQuery.isLoading || model.detailQuery.isFetching) {
    message.info('正在读取附加工艺配置，请稍候')
    return
  }
  if (model.detailQuery.isError) {
    message.error('附加工艺配置读取失败，请刷新后重试')
    return
  }
  const missing = model.serviceOnlyRolls.find((roll) => roll.uuid
    && !model.data.serviceConfigured[roll.uuid])
  if (!missing) {
    props.onNext()
    return
  }
  props.onSelect(missing.localId)
  message.warning(`母卷 ${props.rolls.indexOf(missing) + 1} 尚未配置裁损整理或重新包装`)
}

export function currentServiceSaved(context: ConfigStepCommandContext) {
  const { model, setPreferredEditor } = context
  if (!model.selected) return
  if (model.selected.processMode === 4
    || isConfiguredPlanReady(model.selected, model.data.configuredPlanIds, model.data.previews)) {
    selectNextPending(context, [model.selected.localId])
    return
  }
  setPreferredEditor('plan')
}

function selectNextPending(context: ConfigStepCommandContext, assumedSavedIds: string[]) {
  const { model, props, setPreferredEditor } = context
  if (!model.selected) return
  const next = nextPendingConfigRoll({
    configuredPlanIds: model.data.configuredPlanIds,
    lockedRolls: model.data.lockedRolls,
    previews: model.data.previews,
    routePreviews: model.data.routePreviews,
    rolls: model.data.rolls,
    serviceConfigured: model.data.serviceConfigured,
  }, model.selected.localId, assumedSavedIds)
  if (!next) return
  clearBatchSelections(context)
  if (next.processMode !== 4) setPreferredEditor('plan')
  props.onSelect(next.localId)
}

export function changeEditor(context: ConfigStepCommandContext, editor: ConfigEditorTab) {
  if (context.model.data.activeEditor === editor) return
  clearBatchSelections(context)
  context.setPreferredEditor(editor)
}

export function clearBatchSelections(context: ConfigStepCommandContext) {
  context.selections.plan.clear()
  context.selections.service.clear()
}

import type { ConfigStepWorkspaceActions } from './components/configStepWorkspaceTypes'
import {
  applyToChecked,
  changeEditor,
  continueToNextStep,
  currentServiceSaved,
  previewCurrent,
  runUnlessSaving,
  saveCurrent,
  selectRoll,
  selectSameSpec,
  synchronizeVersion,
  toggleRoll,
  type ConfigStepCommandContext,
} from './configStepActionHandlers'

export function createConfigStepWorkspaceActions(
  context: ConfigStepCommandContext,
): ConfigStepWorkspaceActions {
  const { model, props, selections } = context
  return {
    onApplyChecked: () => applyToChecked(context),
    onClearSelection: () => runUnlessSaving(context,
      model.data.activeEditor === 'service' ? selections.service.clear : selections.plan.clear),
    onCurrentServiceSaved: () => currentServiceSaved(context),
    onEditorChange: (editor) => runUnlessSaving(context, () => changeEditor(context, editor)),
    onNext: () => continueToNextStep(context),
    onOpenRouteDesigner: props.onOpenRouteDesigner,
    onPlanChange: (plan) => runUnlessSaving(context, () => {
      if (model.selected) props.onPlanChange(model.selected.localId, plan)
    }),
    onPrev: () => runUnlessSaving(context, props.onPrev),
    onPreviewCurrent: () => previewCurrent(context),
    onRetryDetail: () => void model.detailQuery.refetch(),
    onSaveCurrent: () => saveCurrent(context),
    onServiceBatchApplied: selections.service.clear,
    onServicePendingChange: props.onPendingChange,
    onSelect: (localId) => selectRoll(context, localId),
    onSelectSameSpec: () => selectSameSpec(context),
    onServiceDirtyChange: props.onServiceDirtyChange,
    onSynchronizeVersion: () => synchronizeVersion(context),
    onToggle: (localId, checked) => toggleRoll(context, localId, checked),
  }
}

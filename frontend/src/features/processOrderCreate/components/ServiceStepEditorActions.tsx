import { CheckOutlined, ReloadOutlined, SaveOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ServiceApplyTargets } from '../serviceStepBatchModel'

interface ActionState {
  analysis: ServiceApplyTargets
  batchSaving: boolean
  disabled: boolean
  saving: boolean
  selectedRollCount: number
  updatingCurrent: boolean
}

interface ActionHandlers {
  onApply: () => void
  onReset: () => void
  onSave: () => void
}

interface Props {
  actions: ActionHandlers
  state: ActionState
}

export default function ServiceStepEditorActions({ actions, state }: Props) {
  const saveReason = saveDisabledReason(state)
  const applyReason = applyDisabledReason(state)
  return (
    <>
      <div className="service-editor-footer__scope">{scopeText(state)}</div>
      <Button icon={<ReloadOutlined />} onClick={actions.onReset}>重置</Button>
      <MesTooltip title={saveReason}>
        <span className="service-editor-action-tooltip" title={saveReason}>
          <Button icon={<SaveOutlined />} loading={state.saving} disabled={Boolean(saveReason)}
            onClick={actions.onSave}>
            {state.updatingCurrent ? '更新当前卷' : '保存当前卷'}
          </Button>
        </span>
      </MesTooltip>
      <MesTooltip title={applyReason}>
        <span className="service-editor-action-tooltip" title={applyReason}>
          <Button type="primary" icon={<CheckOutlined />} loading={state.batchSaving}
            disabled={Boolean(applyReason)} onClick={actions.onApply}>
            应用到选中（{state.analysis.targetUuids.length}卷）
          </Button>
        </span>
      </MesTooltip>
    </>
  )
}

function saveDisabledReason(state: ActionState) {
  return state.disabled ? '工艺目录正在加载，或当前工序不可用' : undefined
}

function applyDisabledReason(state: ActionState) {
  if (state.disabled) return saveDisabledReason(state)
  if (!state.selectedRollCount) return '请先在左侧勾选要应用的母卷'
  if (!state.analysis.targetUuids.length) return '选中母卷未保存或处理方式不符，暂无可应用目标'
  return undefined
}

function scopeText(state: ActionState) {
  if (!state.selectedRollCount) return '左侧尚未勾选母卷'
  if (state.disabled) return `已选 ${state.selectedRollCount} 卷 · 请先完成配置`
  return `已选 ${state.selectedRollCount} 卷 · 新增 ${state.analysis.createCount} · 更新 ${state.analysis.updateCount}`
}

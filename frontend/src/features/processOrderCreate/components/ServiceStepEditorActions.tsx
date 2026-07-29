import { CopyOutlined, SaveOutlined, UndoOutlined } from '@ant-design/icons'
import { Button } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { ServiceApplyTargets } from '../serviceStepBatchModel'

interface ActionState {
  analysis: ServiceApplyTargets
  batchSaving: boolean
  catalogError: boolean
  catalogLoading: boolean
  catalogUnavailable: boolean
  currentRollUuid: string
  dirty: boolean
  saving: boolean
  selectedRollCount: number
  writePending: boolean
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
    <div className="draft-service-editor__command-bar" aria-label="附加工艺操作">
      <div className={`draft-service-editor__scope${catalogBlocked(state) ? '' : ' is-active'}`}>
        <span>批量范围</span>
        <strong>{scopeText(state)}</strong>
      </div>
      <div className="draft-service-editor__actions">
        <Button icon={<UndoOutlined />} disabled={!state.dirty || state.writePending} onClick={actions.onReset}>
          还原未保存修改
        </Button>
        <MesTooltip title={saveReason}>
          <span className="service-editor-action-tooltip" title={saveReason}>
            <Button icon={<SaveOutlined />} loading={state.saving} disabled={Boolean(saveReason)}
              onClick={actions.onSave}>
              保存本卷附加工艺
            </Button>
          </span>
        </MesTooltip>
        <MesTooltip title={applyReason}>
          <span className="service-editor-action-tooltip" title={applyReason}>
            <Button icon={<CopyOutlined />} loading={state.batchSaving}
              disabled={Boolean(applyReason)} onClick={actions.onApply}>
              批量应用附加工艺（{state.analysis.targetUuids.length} 卷）
            </Button>
          </span>
        </MesTooltip>
      </div>
    </div>
  )
}

function saveDisabledReason(state: ActionState) {
  if (state.writePending) return '附加工艺正在保存，请稍候'
  if (state.catalogLoading) return '正在加载可用附加工艺，请稍候'
  if (state.catalogError) return '附加工艺目录加载失败，请先重试'
  if (state.catalogUnavailable) return '请选择当前母卷可用的附加工艺'
  if (!state.dirty) return '当前附加工艺没有未保存修改'
  return undefined
}

function applyDisabledReason(state: ActionState) {
  if (state.writePending) return saveDisabledReason(state)
  if (catalogBlocked(state)) return saveDisabledReason(state)
  if (!state.selectedRollCount) return '请先在左侧勾选要应用的母卷'
  if (!state.analysis.targetUuids.length) return '选中母卷未保存或处理方式不符，暂无可应用目标'
  if (isCurrentRollOnly(state)) return '当前只有本卷，请使用“保存本卷附加工艺”'
  return undefined
}

function scopeText(state: ActionState) {
  if (!state.selectedRollCount) return '未选择批量目标'
  if (catalogBlocked(state)) return `已选 ${state.selectedRollCount} 卷，完成当前配置后可应用`
  return `已选 ${state.selectedRollCount} 卷，可新增 ${state.analysis.createCount}，可更新 ${state.analysis.updateCount}`
}

function catalogBlocked(state: ActionState) {
  return state.catalogLoading || state.catalogError || state.catalogUnavailable
}

function isCurrentRollOnly(state: ActionState) {
  return state.analysis.targetUuids.length === 1
    && state.analysis.targetUuids[0] === state.currentRollUuid
}

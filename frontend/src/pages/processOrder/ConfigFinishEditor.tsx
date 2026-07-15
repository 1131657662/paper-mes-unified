import { Button, Empty, Space } from 'antd'
import { ArrowLeftOutlined, ArrowRightOutlined, CopyOutlined, SaveOutlined } from '@ant-design/icons'
import FinishConfigPanel from '../../components/processOrder/FinishConfigPanel'
import type { FinishConfigSaveDTO, OriginalRoll, ProcessOrderDetailVO } from '../../types/processOrder'

interface EditorState {
  checkedCount: number
  config?: FinishConfigSaveDTO
  currentRoll?: OriginalRoll
  saving: boolean
  selectedIndex: number
}

interface EditorActions {
  onConfigChange: (config: FinishConfigSaveDTO) => void
  onCopy: () => void
  onNext: () => void
  onPrevious: () => void
  onSave: () => void
}

interface Props {
  actions: EditorActions
  detail?: ProcessOrderDetailVO
  state: EditorState
}

export default function ConfigFinishEditor({ actions, detail, state }: Props) {
  const rolls = detail?.originalRolls ?? []
  return (
    <section className="config-finish-editor">
      <div className="config-finish-editor__body">
        {state.currentRoll && detail ? (
          <FinishConfigPanel
            key={state.currentRoll.uuid}
            config={state.config}
            onConfigChange={actions.onConfigChange}
            order={detail.order}
            originalRolls={rolls}
            roll={state.currentRoll}
          />
        ) : <div className="config-finish-empty"><Empty description="请选择母卷进行配置" /></div>}
      </div>
      <EditorFooter actions={actions} rollCount={rolls.length} state={state} />
    </section>
  )
}

function EditorFooter({ actions, rollCount, state }: { actions: EditorActions; rollCount: number; state: EditorState }) {
  return (
    <div className="config-finish-editor__footer">
      <Space>
        <Button icon={<SaveOutlined />} loading={state.saving} type="primary" onClick={actions.onSave}>保存当前配置</Button>
        <Button disabled={state.checkedCount === 0} icon={<CopyOutlined />} onClick={actions.onCopy}>复制到已选母卷 ({state.checkedCount})</Button>
      </Space>
      <Space>
        <Button disabled={state.selectedIndex === 0} icon={<ArrowLeftOutlined />} onClick={actions.onPrevious}>上一卷</Button>
        <Button disabled={state.selectedIndex >= rollCount - 1} icon={<ArrowRightOutlined />} onClick={actions.onNext}>下一卷</Button>
      </Space>
    </div>
  )
}

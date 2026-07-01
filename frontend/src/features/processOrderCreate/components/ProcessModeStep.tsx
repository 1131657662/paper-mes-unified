import { Button, Card, Radio, Select, Space, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import { applyDefaultMachineToRoll } from '../machineDefaults'
import type { RollDraft } from '../types'
import ProcessMachineSelect from './ProcessMachineSelect'
import ResizableWorkspace from './ResizableWorkspace'
import RollSelectorPanel from './RollSelectorPanel'

interface Props {
  rolls: RollDraft[]
  machines: Machine[]
  selectedId?: string
  loading: boolean
  onSelect: (localId: string) => void
  onChange: (rolls: RollDraft[]) => void
  onPrev: () => void
  onNext: () => void
}

const processOptions = Object.entries(PROCESS_MODE).map(([value, label]) => ({ value: Number(value), label }))
const stepOptions = Object.entries(STEP_TYPE).map(([value, label]) => ({ value: Number(value), label }))
const workbenchCardStyle = {
  height: 'max(520px, calc(100vh - 310px))',
  display: 'flex',
  flexDirection: 'column',
} as const

export default function ProcessModeStep({
  rolls,
  machines,
  selectedId,
  loading,
  onSelect,
  onChange,
  onPrev,
  onNext,
}: Props) {
  const selected = rolls.find((roll) => roll.localId === selectedId) ?? rolls[0]

  const patchSelected = (patch: Partial<RollDraft>) => {
    if (!selected) return
    const nextRoll = applyDefaultMachineToRoll({ ...selected, ...patch }, machines)
    onChange(rolls.map((roll) => (roll.localId === selected.localId ? nextRoll : roll)))
  }

  const batchApply = (processMode: number, mainStepType?: number) => {
    onChange(rolls.map((roll) => applyDefaultMachineToRoll({
      ...roll,
      processMode,
      mainStepType: processMode === 3 ? undefined : mainStepType ?? 2,
      machineUuid: processMode === 3 ? undefined : roll.machineUuid,
    }, machines)))
  }

  return (
    <Card
      title="加工方式"
      style={workbenchCardStyle}
      styles={{ body: { flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' } }}
    >
      <div style={{ flex: 1, minHeight: 0 }}>
        <ResizableWorkspace
          leftTitle="原卷列表"
          mainTitle={selected ? `配置：${selected.paperName || selected.rollNo || '未命名原纸'}` : '配置'}
          left={<RollSelectorPanel machines={machines} rolls={rolls} selectedId={selected?.localId} onSelect={onSelect} />}
          main={<ProcessModeEditor machines={machines} selected={selected} patchSelected={patchSelected} batchApply={batchApply} />}
          leftInitial={30}
        />
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
        <Space wrap>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" loading={loading} onClick={onNext}>下一步：工艺配置</Button>
        </Space>
      </div>
    </Card>
  )
}

function ProcessModeEditor({ machines, selected, patchSelected, batchApply }: EditorProps) {
  return (
    <Space direction="vertical" size={18} style={{ width: '100%' }}>
      <Space wrap>
        <Typography.Text strong>批量设置</Typography.Text>
        <Button onClick={() => batchApply(1, 2)}>全部标准复卷</Button>
        <Button onClick={() => batchApply(1, 1)}>全部标准锯纸</Button>
        <Button onClick={() => batchApply(3)}>全部直发</Button>
      </Space>
      <div>
        <Typography.Text strong>加工方式</Typography.Text>
        <Select
          value={selected?.processMode ?? 1}
          options={processOptions}
          style={{ width: 180, marginLeft: 12 }}
          onChange={(value) => patchSelected({ processMode: value, mainStepType: value === 3 ? undefined : selected?.mainStepType ?? 2 })}
        />
      </div>
      {selected?.processMode !== 3 && <MainStepSelector selected={selected} patchSelected={patchSelected} />}
      {selected?.processMode !== 3 && (
        <ProcessMachineSelect
          machines={machines}
          mainStepType={selected?.mainStepType ?? 2}
          value={selected?.machineUuid}
          onChange={(machineUuid) => patchSelected({ machineUuid })}
        />
      )}
      {selected?.processMode === 3 && (
        <Typography.Text type="secondary">直发卷不进入工艺配置，回录时沿用母卷号生成直发成品。</Typography.Text>
      )}
    </Space>
  )
}

function MainStepSelector({ selected, patchSelected }: Pick<EditorProps, 'selected' | 'patchSelected'>) {
  return (
    <div>
      <Typography.Text strong>主工艺</Typography.Text>
      <Radio.Group
        value={selected?.mainStepType ?? 2}
        options={stepOptions}
        optionType="button"
        buttonStyle="solid"
        style={{ marginLeft: 12 }}
        onChange={(event) => patchSelected({ mainStepType: event.target.value })}
      />
    </div>
  )
}

interface EditorProps {
  machines: Machine[]
  selected?: RollDraft
  patchSelected: (patch: Partial<RollDraft>) => void
  batchApply: (processMode: number, mainStepType?: number) => void
}

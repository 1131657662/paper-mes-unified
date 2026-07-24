import { Button, Card, Select, Space, Typography } from 'antd'
import { PROCESS_MODE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import { applyDefaultMachineToRoll } from '../machineDefaults'
import { useProcessModeRollSelection } from '../hooks/useProcessModeRollSelection'
import type { RollDraft } from '../types'
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
  const batchSelection = useProcessModeRollSelection(rolls, selected?.localId)

  const patchSelected = (patch: Partial<RollDraft>) => {
    if (!selected) return
    const nextRoll = applyDefaultMachineToRoll({ ...selected, ...patch }, machines)
    onChange(rolls.map((roll) => (roll.localId === selected.localId ? nextRoll : roll)))
  }

  const batchApply = (processMode: number, mainStepType?: number) => {
    const checkedIds = new Set(batchSelection.checkedIds)
    if (!checkedIds.size) return
    const requiresMain = processModeRequiresMain(processMode)
    onChange(rolls.map((roll) => {
      if (!checkedIds.has(roll.localId)) return roll
      return applyDefaultMachineToRoll({
        ...roll,
        processMode,
        mainStepType: requiresMain ? mainStepType ?? 2 : undefined,
        machineUuid: requiresMain ? roll.machineUuid : undefined,
      }, machines)
    }))
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
          left={<RollSelectorPanel machines={machines} rolls={rolls} selectedId={selected?.localId}
            batchSelection={{ checkedIds: batchSelection.checkedIds, onClear: batchSelection.clear,
              onSelectAll: batchSelection.selectAll, onToggle: batchSelection.toggle }} onSelect={onSelect} />}
          main={<ProcessModeEditor selected={selected} selectedCount={batchSelection.checkedIds.length} patchSelected={patchSelected} batchApply={batchApply} />}
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

function ProcessModeEditor({ selected, selectedCount, patchSelected, batchApply }: EditorProps) {
  return (
    <Space direction="vertical" size={18} style={{ width: '100%' }}>
      <Space wrap>
        <Typography.Text strong>批量应用</Typography.Text>
        <Typography.Text type="secondary">请先在左侧勾选母卷</Typography.Text>
        <Button disabled={!selectedCount} onClick={() => batchApply(1, 2)}>应用到已选 {selectedCount} 卷：标准复卷</Button>
        <Button disabled={!selectedCount} onClick={() => batchApply(1, 1)}>应用到已选 {selectedCount} 卷：标准锯纸</Button>
        <Button disabled={!selectedCount} onClick={() => batchApply(4)}>应用到已选 {selectedCount} 卷：仅附加工艺</Button>
        <Button disabled={!selectedCount} onClick={() => batchApply(3)}>应用到已选 {selectedCount} 卷：直发</Button>
      </Space>
      <div>
        <Typography.Text strong>加工方式</Typography.Text>
        <Select
          aria-label="当前母卷加工方式"
          value={selected?.processMode ?? 1}
          options={processOptions}
          style={{ width: 180, marginLeft: 12 }}
          onChange={(value) => patchSelected({
            processMode: value,
            mainStepType: processModeRequiresMain(value) ? selected?.mainStepType ?? 2 : undefined,
            machineUuid: processModeRequiresMain(value) ? selected?.machineUuid : undefined,
          })}
        />
      </div>
      {selected?.processMode === 3 && (
        <Typography.Text type="secondary">直发卷不进入工艺配置，回录时沿用母卷号生成直发成品。</Typography.Text>
      )}
      {selected?.processMode === 4 && (
        <Typography.Text type="secondary">只执行整理或包装，回录实际成品重量，不设置锯纸或复卷。</Typography.Text>
      )}
    </Space>
  )
}

interface EditorProps {
  selected?: RollDraft
  selectedCount: number
  patchSelected: (patch: Partial<RollDraft>) => void
  batchApply: (processMode: number, mainStepType?: number) => void
}

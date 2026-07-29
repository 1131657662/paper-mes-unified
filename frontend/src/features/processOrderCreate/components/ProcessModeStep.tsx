import { AppstoreAddOutlined, CheckOutlined } from '@ant-design/icons'
import { Button, Card, Segmented, Select, Space, Typography, message } from 'antd'
import { useState } from 'react'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import { applyDefaultMachineToRoll } from '../machineDefaults'
import { useProcessModeRollSelection } from '../hooks/useProcessModeRollSelection'
import { applyProcessModeBatch } from '../processModeBatchModel'
import type { RollDraft } from '../types'
import ResizableWorkspace from './ResizableWorkspace'
import RollSelectorPanel from './RollSelectorPanel'
import './ProcessModeStep.css'

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
const mainStepOptions = [1, 2].map((value) => ({ value, label: STEP_TYPE[value] }))
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
  const [batchMode, setBatchMode] = useState(false)

  const patchSelected = (patch: Partial<RollDraft>) => {
    if (loading || !selected) return
    const nextRoll = applyDefaultMachineToRoll({ ...selected, ...patch }, machines)
    onChange(rolls.map((roll) => (roll.localId === selected.localId ? nextRoll : roll)))
  }

  const batchApply = () => {
    if (loading || !selected || !batchSelection.checkedIds.length) return
    onChange(applyProcessModeBatch({
      checkedIds: batchSelection.checkedIds,
      machines,
      mainStepType: selected.mainStepType,
      processMode: selected.processMode ?? 1,
      rolls,
    }))
    message.success(`已应用到 ${batchSelection.checkedIds.length} 卷，待统一保存`)
    batchSelection.clear()
  }

  return (
    <Card
      className="process-mode-workbench"
      title="加工方式"
      extra={<Button
        icon={batchMode ? <CheckOutlined /> : <AppstoreAddOutlined />}
        disabled={loading}
        type={batchMode ? 'primary' : 'default'}
        onClick={() => {
          if (batchMode) batchSelection.clear()
          setBatchMode(!batchMode)
        }}
      >{batchMode ? '退出批量设置' : '批量设置'}</Button>}
      styles={{ body: { flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' } }}
    >
      <div style={{ flex: 1, minHeight: 0 }}>
        <ResizableWorkspace
          leftTitle="原卷列表"
          mainTitle={selected ? `配置：${selected.paperName || selected.rollNo || '未命名原纸'}` : '配置'}
          left={<RollSelectorPanel disabled={loading} machines={machines} rolls={rolls} selectedId={selected?.localId}
            batchSelection={batchMode ? { checkedIds: batchSelection.checkedIds, onClear: batchSelection.clear,
              onSelectAll: batchSelection.selectAll, onToggle: batchSelection.toggle } : undefined}
            onSelect={(localId) => {
              if (loading) return
              if (selected?.localId !== localId) batchSelection.clear()
              onSelect(localId)
            }} />}
          main={<ProcessModeEditor batchMode={batchMode} selected={selected}
            disabled={loading} selectedCount={batchSelection.checkedIds.length}
            patchSelected={patchSelected} batchApply={batchApply} />}
          leftInitial={30}
        />
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
        <Space wrap>
          <Button disabled={loading} onClick={onPrev}>上一步</Button>
          <Button type="primary" loading={loading} onClick={onNext}>下一步：工艺配置</Button>
        </Space>
      </div>
    </Card>
  )
}

function ProcessModeEditor({ batchMode, disabled, selected, selectedCount, patchSelected, batchApply }: EditorProps) {
  const processMode = selected?.processMode ?? 1
  return (
    <Space direction="vertical" size={18} style={{ width: '100%' }}>
      <div>
        <Typography.Text strong>加工方式</Typography.Text>
        <Select
          aria-label="当前母卷加工方式"
          disabled={disabled}
          value={processMode}
          options={processOptions}
          style={{ width: 180, marginLeft: 12 }}
          onChange={(value) => patchSelected({
            processMode: value,
            mainStepType: processModeRequiresMain(value) ? selected?.mainStepType ?? 2 : undefined,
            machineUuid: processModeRequiresMain(value) ? selected?.machineUuid : undefined,
          })}
        />
      </div>
      {processModeRequiresMain(processMode) && <Space wrap>
        <Typography.Text strong>主工艺</Typography.Text>
        <Segmented
          aria-label="当前母卷主工艺"
          disabled={disabled}
          value={selected?.mainStepType ?? 2}
          options={mainStepOptions}
          onChange={(value) => patchSelected({ mainStepType: Number(value) })}
        />
      </Space>}
      {batchMode && <div className="process-mode-batch-command">
        <div>
          <Typography.Text strong>批量应用当前设置</Typography.Text>
          <Typography.Text type="secondary">左侧已选 {selectedCount} 个目标卷，不含当前卷</Typography.Text>
        </div>
        <Button type="primary" disabled={disabled || !selectedCount} onClick={batchApply}>
          应用到已选 {selectedCount} 卷
        </Button>
      </div>}
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
  batchMode: boolean
  disabled: boolean
  selected?: RollDraft
  selectedCount: number
  patchSelected: (patch: Partial<RollDraft>) => void
  batchApply: () => void
}

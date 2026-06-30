import { useEffect, useRef, useState } from 'react'
import { Button, Card, Empty, Space, message } from 'antd'
import type { PlanPreviewVO, ProcessPlanDTO } from '../../../types/processOrder'
import { defaultPlanForRoll } from '../draftMappers'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import PlanPreviewPanel from './PlanPreviewPanel'
import ProcessPlanEditor from './ProcessPlanEditor'
import ResizableWorkspace from './ResizableWorkspace'
import WorkbenchRollList from './WorkbenchRollList'

const workbenchCardStyle = {
  height: 'max(580px, calc(100vh - 310px))',
  display: 'flex',
  flexDirection: 'column',
} as const

interface Props {
  defaultSpareCount?: number
  orderUuid?: string
  rolls: RollDraft[]
  selectedId?: string
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  saving: boolean
  onSelect: (localId: string) => void
  onPlanChange: (localId: string, plan: ProcessPlanDTO) => void
  onPreviewPlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<void>
  onSavePlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<void>
  onSavePlanBatch: (rolls: RollDraft[], plan: ProcessPlanDTO) => Promise<void>
  onPrev: () => void
  onNext: () => void
}

export default function ConfigStep({
  defaultSpareCount = 0,
  orderUuid,
  rolls,
  selectedId,
  plans,
  previews,
  saving,
  onSelect,
  onPlanChange,
  onPreviewPlan,
  onSavePlan,
  onSavePlanBatch,
  onPrev,
  onNext,
}: Props) {
  const lockedRolls = mergedSourceLocks(rolls, plans)
  const selected = rolls.find((roll) => roll.localId === selectedId && !lockedRolls[roll.localId])
    ?? rolls.find((roll) => roll.processMode !== 3 && !lockedRolls[roll.localId])
  const [checkedIds, setCheckedIds] = useState<string[]>(selected ? [selected.localId] : [])
  const selectedPlan = selected ? plans[selected.localId] ?? defaultPlanForRoll(selected, { spareCount: defaultSpareCount }) : undefined
  useAutoPlanPreview({ orderUuid, selected, selectedPlan, onPreviewPlan })

  const toggle = (localId: string, checked: boolean) => {
    if (lockedRolls[localId]) return
    setCheckedIds((prev) => (checked ? Array.from(new Set([...prev, localId])) : prev.filter((id) => id !== localId)))
  }

  const selectSameSpec = () => {
    if (!selected) return
    const ids = rolls
      .filter((roll) => roll.paperName === selected.paperName && roll.gramWeight === selected.gramWeight && roll.originalWidth === selected.originalWidth)
      .filter((roll) => !lockedRolls[roll.localId])
      .map((roll) => roll.localId)
    setCheckedIds(ids)
  }

  const requireReady = () => {
    if (!orderUuid || !selected || !selected.uuid || !selectedPlan) {
      message.warning('请先保存原纸明细')
      return false
    }
    return true
  }

  const saveCurrent = async () => {
    if (!requireReady() || !selected || !selectedPlan) return
    await onSavePlan(selected, selectedPlan)
  }

  const previewCurrent = async () => {
    if (!requireReady() || !selected || !selectedPlan) return
    await onPreviewPlan(selected, selectedPlan)
  }

  const applyToChecked = async () => {
    if (!selectedPlan) return
    const targets = rolls.filter((roll) => checkedIds.includes(roll.localId) && !lockedRolls[roll.localId] && roll.uuid)
    if (!targets.length) {
      message.warning('请选择已保存的母卷')
      return
    }
    await onSavePlanBatch(targets, selectedPlan)
  }

  const rollList = (
    <WorkbenchRollList
      rolls={rolls}
      selectedId={selected?.localId}
      checkedIds={checkedIds}
      previews={previews}
      lockedRolls={lockedRolls}
      onClearSelection={() => setCheckedIds([])}
      onSelect={selectRoll}
      onLockedSelect={(_, lock) => message.info(`该母卷已被 ${lock.ownerLabel} 合并使用，无需单独配置`)}
      onToggle={toggle}
      onSelectSameSpec={selectSameSpec}
    />
  )

  const editor = selected && selectedPlan ? (
    <ProcessPlanEditor
      roll={selected}
      rolls={rolls}
      plan={selectedPlan}
      defaultSpareCount={defaultSpareCount}
      onChange={(plan) => onPlanChange(selected.localId, plan)}
    />
  ) : (
    <Empty description="请选择母卷" />
  )

  return (
    <Card
      title="母卷加工方案工作台"
      style={workbenchCardStyle}
      styles={{ body: { flex: 1, minHeight: 0, display: 'flex', flexDirection: 'column', overflow: 'hidden' } }}
    >
      <div style={{ flex: 1, minHeight: 0 }}>
        <ResizableWorkspace
          leftTitle="母卷列表"
          mainTitle="工艺配置"
          rightTitle="后端预览"
          left={rollList}
          main={editor}
          right={<PlanPreviewPanel preview={selected ? previews[selected.localId] : undefined} loading={saving} onPreview={previewCurrent} />}
          leftInitial={24}
          rightInitial={30}
        />
      </div>
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
        <Space wrap>
          <Button onClick={onPrev}>上一步</Button>
          <Button loading={saving} onClick={saveCurrent}>保存当前</Button>
          <Button loading={saving} onClick={applyToChecked}>应用到选中</Button>
          <Button type="primary" onClick={onNext}>下一步：预览确认</Button>
        </Space>
      </div>
    </Card>
  )

  function selectRoll(localId: string) {
    if (lockedRolls[localId]) {
      message.info(`该母卷已被 ${lockedRolls[localId].ownerLabel} 合并使用，无需单独配置`)
      return
    }
    onSelect(localId)
    setCheckedIds((prev) => {
      if (selected?.localId !== localId) return [localId]
      return prev.includes(localId) ? prev.filter((id) => id !== localId) : [localId]
    })
  }
}

function useAutoPlanPreview({ orderUuid, selected, selectedPlan, onPreviewPlan }: AutoPreviewOptions) {
  const previewRef = useRef(onPreviewPlan)
  const rollRef = useRef(selected)
  const planRef = useRef(selectedPlan)
  const planFingerprint = selectedPlan ? JSON.stringify(selectedPlan) : ''
  const selectedLocalId = selected?.localId
  const selectedUuid = selected?.uuid

  useEffect(() => {
    previewRef.current = onPreviewPlan
  }, [onPreviewPlan])

  useEffect(() => {
    rollRef.current = selected
    planRef.current = selectedPlan
  }, [selected, selectedPlan])

  useEffect(() => {
    if (!orderUuid || !selectedUuid || !planFingerprint) return
    const timer = window.setTimeout(() => {
      const roll = rollRef.current
      const plan = planRef.current
      if (!roll?.uuid || !plan) return
      previewRef.current(roll, plan).catch(() => undefined)
    }, 700)
    return () => window.clearTimeout(timer)
  }, [orderUuid, selectedLocalId, selectedUuid, planFingerprint])
}

interface AutoPreviewOptions {
  orderUuid?: string
  selected?: RollDraft
  selectedPlan?: ProcessPlanDTO
  onPreviewPlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<void>
}

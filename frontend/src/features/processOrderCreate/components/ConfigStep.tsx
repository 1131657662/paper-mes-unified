import { useEffect, useRef, useState } from 'react'
import { Button, Card, Empty, Space, Table, Tag, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import { defaultPlanForRoll, type DefaultPlanOptions } from '../draftMappers'
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
  defaultPlanOptions?: DefaultPlanOptions
  orderUuid?: string
  machines: Machine[]
  rolls: RollDraft[]
  selectedId?: string
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  saving: boolean
  onOpenRouteDesigner: (roll: RollDraft) => void
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
  defaultPlanOptions,
  orderUuid,
  machines,
  rolls,
  selectedId,
  plans,
  previews,
  routePreviews,
  saving,
  onOpenRouteDesigner,
  onSelect,
  onPlanChange,
  onPreviewPlan,
  onSavePlan,
  onSavePlanBatch,
  onPrev,
  onNext,
}: Props) {
  const lockedRolls = mergedSourceLocks(rolls, plans)
  const configurableRolls = rolls.filter((roll) => roll.processMode !== 3 && !lockedRolls[roll.localId])
  const selected = rolls.find((roll) => roll.localId === selectedId && !lockedRolls[roll.localId])
    ?? rolls.find((roll) => roll.processMode !== 3 && !lockedRolls[roll.localId])
  const [checkedIds, setCheckedIds] = useState<string[]>(selected ? [selected.localId] : [])
  const planDefaults = defaultPlanOptions ?? { spareCount: defaultSpareCount }
  const selectedPlan = selected ? plans[selected.localId] ?? defaultPlanForRoll(selected, planDefaults) : undefined
  const selectedRoutePreview = selected?.uuid ? routePreviews[selected.uuid] : undefined
  useAutoPlanPreview({ orderUuid, selected: selectedRoutePreview ? undefined : selected, selectedPlan, onPreviewPlan })

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

  if (rolls.length > 0 && configurableRolls.length === 0) {
    return (
      <LightConfigStep
        lockedRolls={lockedRolls}
        onNext={onNext}
        onPrev={onPrev}
        rolls={rolls}
      />
    )
  }

  const rollList = (
    <WorkbenchRollList
      machines={machines}
      rolls={rolls}
      selectedId={selected?.localId}
      checkedIds={checkedIds}
      previews={previews}
      routePreviews={routePreviews}
      lockedRolls={lockedRolls}
      onClearSelection={() => setCheckedIds([])}
      onSelect={selectRoll}
      onLockedSelect={(_, lock) => message.info(`该母卷已被 ${lock.ownerLabel} 合并使用，无需单独配置`)}
      onToggle={toggle}
      onSelectSameSpec={selectSameSpec}
      onOpenRouteDesigner={onOpenRouteDesigner}
    />
  )

  const editor = selected && selectedRoutePreview ? (
    <RouteConfiguredPanel
      preview={selectedRoutePreview}
      roll={selected}
      onOpen={() => onOpenRouteDesigner(selected)}
    />
  ) : selected && selectedPlan ? (
    <ProcessPlanEditor
      roll={selected}
      rolls={rolls}
      machines={machines}
      plan={selectedPlan}
      defaultSpareCount={defaultSpareCount}
      defaultPlanOptions={planDefaults}
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

function RouteConfiguredPanel({ onOpen, preview, roll }: RouteConfiguredPanelProps) {
  const finals = preview.outputs?.filter((item) => !item.consumedByNextStage) ?? []
  const finishWeight = finals.reduce((sum, item) => sum + Number(item.estimateWeight ?? 0), 0)
  return (
    <div className="route-configured-panel">
      <Tag color="blue">链式工艺</Tag>
      <Typography.Title level={5}>{roll.rollNo || roll.paperName || '母卷'} 已配置多序加工</Typography.Title>
      <Space wrap>
        <Tag>工序 {preview.stages?.length ?? 0} 道</Tag>
        <Tag color="green">最终成品 {finals.length} 件</Tag>
        <Tag color="cyan">预估 {formatKg(finishWeight)}</Tag>
      </Space>
      <Typography.Paragraph type="secondary">
        该母卷已进入链式路线模式，单道工艺编辑已锁定，避免覆盖多序产物关系。
      </Typography.Paragraph>
      <Button type="primary" onClick={onOpen}>进入链式工艺设计</Button>
    </div>
  )
}

interface RouteConfiguredPanelProps {
  onOpen: () => void
  preview: ProcessRoutePreviewVO
  roll: RollDraft
}

function LightConfigStep({
  lockedRolls,
  onNext,
  onPrev,
  rolls,
}: {
  lockedRolls: ReturnType<typeof mergedSourceLocks>
  onNext: () => void
  onPrev: () => void
  rolls: RollDraft[]
}) {
  return (
    <Card title="无需单独配置的母卷" className="config-light-step">
      <Table
        size="small"
        rowKey="localId"
        pagination={false}
        columns={lightColumns(lockedRolls)}
        dataSource={rolls}
        scroll={{ x: 760 }}
      />
      <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 12 }}>
        <Space wrap>
          <Button onClick={onPrev}>上一步</Button>
          <Button type="primary" onClick={onNext}>下一步：预览确认</Button>
        </Space>
      </div>
    </Card>
  )
}

function lightColumns(lockedRolls: ReturnType<typeof mergedSourceLocks>): ColumnsType<RollDraft> {
  return [
    { title: '母卷', width: 170, render: (_, roll) => rollNoText(roll) },
    { title: '品名', dataIndex: 'paperName', width: 130 },
    { title: '规格', width: 150, render: (_, roll) => `${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)}` },
    { title: '重量', width: 120, align: 'right', render: (_, roll) => formatKg(rollTotalWeight(roll)) },
    { title: '处理方式', width: 140, render: (_, roll) => lightRollStatus(roll, lockedRolls) },
    { title: '说明', width: 220, render: (_, roll) => lightRollHint(roll, lockedRolls) },
  ]
}

function lightRollStatus(roll: RollDraft, lockedRolls: ReturnType<typeof mergedSourceLocks>) {
  if (lockedRolls[roll.localId]) return <Tag color="blue">合并来源</Tag>
  if (roll.processMode === 3) return <Tag color="green">直发</Tag>
  return <Tag>无需配置</Tag>
}

function lightRollHint(roll: RollDraft, lockedRolls: ReturnType<typeof mergedSourceLocks>) {
  const lock = lockedRolls[roll.localId]
  if (lock) return `已由 ${lock.ownerLabel} 合并配置`
  if (roll.processMode === 3) return '回录阶段沿用母卷信息生成直发成品'
  return '-'
}

function rollNoText(roll: RollDraft) {
  return [roll.rollNo, roll.extraNo].filter(Boolean).join(' / ') || roll.paperName || '未编号母卷'
}

function rollTotalWeight(roll: RollDraft) {
  return Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)
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

import { useState } from 'react'
import { message } from 'antd'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { Machine } from '../../../types/machine'
import type { PlanPreviewVO, ProcessPlanDTO, ProcessRoutePreviewVO } from '../../../types/processOrder'
import { useProcessOrderDetail } from '../../processOrderDetail/hooks/useProcessOrderDetail'
import { defaultPlanForRoll, type DefaultPlanOptions } from '../draftMappers'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import type { RollDraft } from '../types'
import { calculateRollWeightBalance } from '../weightBalanceModel'
import ConfigStepLight from './ConfigStepLight'
import ConfigStepWorkspace from './ConfigStepWorkspace'
import { useAutoPlanPreview } from '../hooks/useAutoPlanPreview'
import './DraftAdditionalProcesses.css'
import './ServiceOnlyConfigEditor.css'
import './ConfigStep.css'

interface Props {
  defaultSpareCount?: number
  defaultPlanOptions?: DefaultPlanOptions
  orderUuid?: string
  customerPrices?: CustomerProcessPrice[]
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
  customerPrices,
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
  const [serviceEditorStatus, setServiceEditorStatus] = useState<ServiceEditorStatus>()
  const planDefaults = defaultPlanOptions ?? { spareCount: defaultSpareCount }
  const selectedPlan = selected ? plans[selected.localId] ?? defaultPlanForRoll(selected, planDefaults) : undefined
  const selectedRolls = rolls.filter((roll) => checkedIds.includes(roll.localId))
  const selectedServiceRolls = selectedRolls.filter((roll) => roll.processMode !== 3 && !lockedRolls[roll.localId])
  const serviceOnly = selected?.processMode === 4
  const detailQuery = useProcessOrderDetail(orderUuid, { enabled: Boolean(orderUuid) })
  const allSteps = detailQuery.data?.steps ?? []
  const serviceConfigured = Object.fromEntries(
    rolls.filter((roll) => roll.uuid).map((roll) => [roll.uuid!, serviceStepsForRoll(allSteps, roll.uuid).length > 0]),
  )
  const serviceOnlyRolls = configurableRolls.filter((roll) => roll.processMode === 4)
  const selectedRoutePreview = selected?.uuid ? routePreviews[selected.uuid] : undefined
  const selectedBalance = selected ? calculateRollWeightBalance({
    roll: selected,
    rolls,
    plan: selectedPlan,
    preview: previews[selected.localId],
    routePreview: selectedRoutePreview,
  }) : undefined
  useAutoPlanPreview({
    orderUuid,
    selected: selectedRoutePreview || serviceOnly ? undefined : selected,
    selectedPlan,
    onPreviewPlan,
  })

  const toggle = (localId: string, checked: boolean) => {
    if (lockedRolls[localId]) return
    setCheckedIds((prev) => (checked ? Array.from(new Set([...prev, localId])) : prev.filter((id) => id !== localId)))
  }

  const selectSameSpec = () => {
    if (!selected) return
    if (selected.processMode === 4) {
      setCheckedIds(configurableRolls
        .filter((roll) => roll.processMode === 4 && roll.uuid)
        .map((roll) => roll.localId))
      return
    }
    const ids = rolls
      .filter((roll) => roll.processMode === selected.processMode
        && roll.mainStepType === selected.mainStepType
        && roll.paperName === selected.paperName
        && roll.gramWeight === selected.gramWeight
        && roll.originalWidth === selected.originalWidth)
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
    const targets = rolls.filter((roll) => checkedIds.includes(roll.localId)
      && !lockedRolls[roll.localId]
      && roll.processMode === selected?.processMode
      && roll.mainStepType === selected?.mainStepType
      && roll.uuid)
    if (!targets.length) {
      message.warning('请选择已保存的母卷')
      return
    }
    await onSavePlanBatch(targets, selectedPlan)
  }

  if (rolls.length > 0 && configurableRolls.length === 0) {
    return (
      <ConfigStepLight
        lockedRolls={lockedRolls}
        onNext={onNext}
        onPrev={onPrev}
        rolls={rolls}
      />
    )
  }

  return (
    <ConfigStepWorkspace
      data={{
        allSteps,
        balance: selectedBalance,
        checkedIds,
        customerPrices,
        defaultSpareCount,
        detailError: detailQuery.isError,
        detailLoading: detailQuery.isLoading,
        lockedRolls,
        machines,
        orderUuid,
        plan: selectedPlan,
        planDefaults,
        previews,
        roll: selected,
        rolls,
        routePreview: selectedRoutePreview,
        routePreviews,
        saving,
        selectedServiceRolls,
        serviceConfigured,
        serviceEditorStatus,
        serviceOnly,
      }}
      actions={{
        onApplyChecked: applyToChecked,
        onClearSelection: () => setCheckedIds([]),
        onNext: handleNext,
        onOpenRouteDesigner,
        onPlanChange: (plan) => selected && onPlanChange(selected.localId, plan),
        onPrev,
        onPreviewCurrent: previewCurrent,
        onRetryDetail: () => void detailQuery.refetch(),
        onSaveCurrent: saveCurrent,
        onSelect: selectRoll,
        onSelectSameSpec: selectSameSpec,
        onServiceStatusChange: setServiceEditorStatus,
        onToggle: toggle,
      }}
    />
  )

  function selectRoll(localId: string) {
    if (lockedRolls[localId]) {
      message.info(`该母卷已被 ${lockedRolls[localId].ownerLabel} 合并使用，无需单独配置`)
      return
    }
    if (localId !== selected?.localId) setServiceEditorStatus(undefined)
    onSelect(localId)
  }

  function handleNext() {
    if (detailQuery.isLoading) {
      message.info('正在读取附加工艺配置，请稍候')
      return
    }
    if (detailQuery.isError) {
      message.error('附加工艺配置读取失败，请刷新后重试')
      return
    }
    if (serviceEditorStatus?.dirty) {
      message.warning('当前有尚未应用的附加工艺修改，请先保存当前卷或应用到选中')
      return
    }
    const missing = serviceOnlyRolls.find((roll) => roll.uuid && !serviceConfigured[roll.uuid])
    if (missing) {
      onSelect(missing.localId)
      setCheckedIds((prev) => prev.includes(missing.localId) ? prev : [...prev, missing.localId])
      message.warning(`母卷 ${rolls.indexOf(missing) + 1} 尚未配置剥损整理或重新包装`)
      return
    }
    onNext()
  }
}

import { useState } from 'react'
import { message } from 'antd'
import { useProcessOrderDetail } from '../../processOrderDetail/hooks/useProcessOrderDetail'
import { defaultPlanForRoll } from '../draftMappers'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import { serviceStepsForRoll } from '../serviceStepBatchModel'
import { freshDraftVersion } from '../serviceVersionSync'
import { calculateRollWeightBalance } from '../weightBalanceModel'
import {
  configurableRolls as getConfigurableRolls,
  planBatchTargets,
  sameSpecRollIds,
  selectedConfigRoll,
} from '../configStepSelection'
import ConfigStepLight from './ConfigStepLight'
import ConfigStepWorkspace from './ConfigStepWorkspace'
import type { ConfigStepProps } from './configStepTypes'
import { useAutoPlanPreview } from '../hooks/useAutoPlanPreview'
import './DraftAdditionalProcesses.css'
import './ServiceOnlyConfigEditor.css'
import './ConfigStep.css'

export default function ConfigStep({
  defaultSpareCount = 0,
  defaultPlanOptions,
  orderUuid,
  customerPrices,
  machines,
  draftVersion,
  rolls,
  selectedId,
  configuredPlanIds,
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
  onServiceDirtyChange,
  onDraftVersionChange,
  onPrev,
  onNext,
}: ConfigStepProps) {
  const lockedRolls = mergedSourceLocks(rolls, plans)
  const configurableRolls = getConfigurableRolls(rolls, lockedRolls)
  const selected = selectedConfigRoll(rolls, selectedId, lockedRolls)
  const [checkedIds, setCheckedIds] = useState<string[]>(selected ? [selected.localId] : [])
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
  const batchTargets = planBatchTargets({ checkedIds, locks: lockedRolls, rolls, routePreviews, selected })
  const batchOnlyCurrent = batchTargets.length === 1
    && batchTargets[0]?.localId === selected?.localId
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
    setCheckedIds(sameSpecRollIds({ locks: lockedRolls, rolls, selected }))
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

  const synchronizeVersion = async () => {
    const result = await detailQuery.refetch({ cancelRefetch: false })
    const nextVersion = freshDraftVersion(result.data, draftVersion)
    if (nextVersion == null) {
      message.error('附加工艺已提交，但草稿版本刷新失败；请重试同步后再继续')
      throw new Error('draft version synchronization failed')
    }
    onDraftVersionChange(nextVersion)
  }

  const applyToChecked = async () => {
    if (!selectedPlan) return
    if (!batchTargets.length || batchOnlyCurrent) {
      message.warning('请勾选至少 1 卷其他兼容且已保存的母卷')
      return
    }
    await onSavePlanBatch(batchTargets, selectedPlan)
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
      key={selected?.localId ?? 'no-selection'}
      data={{
        allSteps,
        balance: selectedBalance,
        checkedIds,
        configuredPlanIds,
        customerPrices,
        defaultSpareCount,
        detailError: detailQuery.isError,
        detailLoading: detailQuery.isLoading,
        lockedRolls,
        machines,
        mainBatchOnlyCurrent: batchOnlyCurrent,
        mainBatchTargetCount: batchTargets.length,
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
        onServiceDirtyChange,
        onSynchronizeVersion: synchronizeVersion,
        onToggle: toggle,
      }}
    />
  )

  function selectRoll(localId: string) {
    if (lockedRolls[localId]) {
      message.info(`该母卷已被 ${lockedRolls[localId].ownerLabel} 合并使用，无需单独配置`)
      return
    }
    onSelect(localId)
  }

  function handleNext() {
    if (detailQuery.isLoading || detailQuery.isFetching) {
      message.info('正在读取附加工艺配置，请稍候')
      return
    }
    if (detailQuery.isError) {
      message.error('附加工艺配置读取失败，请刷新后重试')
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

import { useEffect, useRef, useState } from 'react'
import { message } from 'antd'
import type { DraftOrderBaseDTO, OriginalRollImportPreviewVO, PlanPreviewVO, ProcessOrderSubmitVO, ProcessPlanDTO } from '../../../types/processOrder'
import { hydrateDraftState, isRollReadyForSave, plansForRolls, plansFromBatch, previewsFromBatch, rebasePlanForRoll } from '../createOrderState'
import { attachSavedUuids, defaultPlanForRoll, newRollDraft, normalizeBaseInfo, toRollDto } from '../draftMappers'
import { mergedSourceUuidSet } from '../rewindConsumptionUtils'
import type { RollDraft } from '../types'
import { useCreateDraft } from './useCreateDraft'
import { useGetDraft } from './useGetDraft'
import { useImportPreview } from './useImportPreview'
import { usePreviewPlan } from './usePreviewPlan'
import { useCustomers, useWarehouses } from './useReferenceData'
import { useReplaceRolls } from './useReplaceRolls'
import { useSaveBaseInfo } from './useSaveBaseInfo'
import { useSavePlan } from './useSavePlan'
import { useSavePlanBatch } from './useSavePlanBatch'
import { useSaveProgress } from './useSaveProgress'
import { useSubmitDraft } from './useSubmitDraft'
import { useUpdateRoll } from './useUpdateRoll'
import type { Customer } from '../../../types/customer'

export function useCreateOrderPage(draftUuid?: string) {
  const hydratedDraftUuid = useRef<string>()
  const [current, setCurrent] = useState(0)
  const [orderUuid, setOrderUuid] = useState<string>()
  const [baseInfo, setBaseInfo] = useState<DraftOrderBaseDTO>()
  const [rolls, setRolls] = useState<RollDraft[]>([newRollDraft()])
  const [selectedId, setSelectedId] = useState<string>()
  const [plans, setPlans] = useState<Record<string, ProcessPlanDTO>>({})
  const [previews, setPreviews] = useState<Record<string, PlanPreviewVO>>({})
  const [submitResult, setSubmitResult] = useState<ProcessOrderSubmitVO>()

  const { data: draft, isLoading: loadingDraft } = useGetDraft(draftUuid)
  const { data: customerPage } = useCustomers()
  const { data: warehousePage } = useWarehouses()
  const { mutateAsync: createDraft, isPending: creatingDraft } = useCreateDraft()
  const { mutateAsync: saveBaseInfo, isPending: savingBase } = useSaveBaseInfo()
  const { mutateAsync: saveProgress } = useSaveProgress()
  const { mutateAsync: replaceRolls, isPending: savingRolls } = useReplaceRolls()
  const { mutateAsync: updateRoll, isPending: updatingRolls } = useUpdateRoll()
  const { mutateAsync: importPreview, isPending: importingRolls } = useImportPreview()
  const { mutateAsync: previewPlan, isPending: previewingPlan } = usePreviewPlan()
  const { mutateAsync: savePlan, isPending: savingPlan } = useSavePlan()
  const { mutateAsync: savePlanBatch, isPending: savingPlanBatch } = useSavePlanBatch()
  const { mutateAsync: submitDraft, isPending: submitting } = useSubmitDraft()

  useEffect(() => {
    if (!draftUuid || !draft || hydratedDraftUuid.current === draftUuid) return
    const state = hydrateDraftState(draft)
    setOrderUuid(state.orderUuid ?? draftUuid)
    setBaseInfo(state.baseInfo)
    setRolls(state.rolls)
    setPlans(state.plans)
    setPreviews(state.previews)
    setSelectedId(state.selectedId)
    setCurrent(state.current)
    hydratedDraftUuid.current = draftUuid
  }, [draft, draftUuid])

  const moveToStep = async (nextStep: number, uuid = orderUuid) => {
    if (uuid) await saveProgress({ uuid, currentStep: nextStep })
    setCurrent(nextStep)
  }

  const handleBaseNext = async (value: DraftOrderBaseDTO) => {
    const dto = normalizeBaseInfo(value)
    const uuid = orderUuid ?? await createDraft(dto)
    if (orderUuid) await saveBaseInfo({ uuid: orderUuid, dto })
    setOrderUuid(uuid)
    setBaseInfo(dto)
    await moveToStep(1, uuid)
  }

  const handleRollsNext = async () => {
    if (!orderUuid) return
    if (rolls.some((roll) => !isRollReadyForSave(roll))) {
      message.warning('请补全品名和单重')
      return
    }
    const uuids = await replaceRolls({ uuid: orderUuid, rolls: rolls.map(toRollDto) })
    const savedRolls = attachSavedUuids(rolls, uuids)
    setRolls(savedRolls)
    setPlans(plansForRolls(savedRolls, {}))
    setPreviews({})
    setSelectedId(savedRolls[0]?.localId)
    await moveToStep(2)
  }

  const handleProcessNext = async () => {
    await Promise.all(
      rolls.filter((roll) => roll.uuid).map((roll) => updateRoll({ rollUuid: roll.uuid!, dto: toRollDto(roll) })),
    )
    setPlans(plansForRolls(rolls, plans))
    setPreviews({})
    await moveToStep(3)
  }

  const handleImportPreview = async (file: File): Promise<OriginalRollImportPreviewVO> => {
    if (!orderUuid) {
      message.warning('请先保存基础信息')
      return { validRows: [], errors: [{ rowNumber: 0, message: '请先保存基础信息' }] }
    }
    return importPreview({ uuid: orderUuid, file })
  }

  const handlePlanChange = (localId: string, plan: ProcessPlanDTO) => {
    setPlans((prev) => ({ ...prev, [localId]: plan }))
  }

  const saveRollPlan = async (roll: RollDraft, plan: ProcessPlanDTO, notify = false) => {
    if (!orderUuid || !roll.uuid) return
    const nextPlan = rebasePlanForRoll(plan, roll)
    const preview = await savePlan({
      orderUuid,
      rollUuid: roll.uuid,
      request: { originalUuid: roll.uuid, plan: nextPlan },
    })
    setPlans((prev) => ({ ...prev, [roll.localId]: nextPlan }))
    setPreviews((prev) => ({ ...prev, [roll.localId]: preview }))
    if (notify) message.success('方案已保存')
  }

  const handlePreviewPlan = async (roll: RollDraft, plan: ProcessPlanDTO) => {
    if (!orderUuid || !roll.uuid) return
    const nextPlan = rebasePlanForRoll(plan, roll)
    const preview = await previewPlan({
      orderUuid,
      request: { originalUuid: roll.uuid, plan: nextPlan },
    })
    setPlans((prev) => ({ ...prev, [roll.localId]: nextPlan }))
    setPreviews((prev) => ({ ...prev, [roll.localId]: preview }))
  }

  const handleSavePlan = (roll: RollDraft, plan: ProcessPlanDTO) => {
    return saveRollPlan(roll, plan, true)
  }

  const handleSavePlanBatch = async (targetRolls: RollDraft[], plan: ProcessPlanDTO) => {
    if (!orderUuid) return
    const savedRolls = targetRolls.filter((roll) => roll.uuid)
    const result = await savePlanBatch({
      orderUuid,
      dto: { originalUuids: savedRolls.map((roll) => roll.uuid!), plan },
    })
    setPlans((prev) => ({ ...prev, ...plansFromBatch(savedRolls, plan) }))
    setPreviews((prev) => ({ ...prev, ...previewsFromBatch(savedRolls, result) }))
    message.success(`已应用到 ${savedRolls.length} 卷母卷`)
  }

  const handleConfigNext = async () => {
    const mergedSourceUuids = mergedSourceUuidSet(rolls, plans)
    for (const roll of rolls.filter((item) => item.processMode !== 3)) {
      if (roll.uuid && mergedSourceUuids.has(roll.uuid)) continue
      await saveRollPlan(roll, plans[roll.localId] ?? defaultPlanForRoll(roll))
    }
    await moveToStep(4)
  }

  const handleSubmit = async () => {
    if (!orderUuid) return
    const result = await submitDraft(orderUuid)
    setSubmitResult(result)
    message.success(`加工单 ${result.orderNo} 已提交`)
  }

  return {
    baseInfo,
    current,
    loadingDraft,
    orderUuid,
    plans,
    previews,
    rolls,
    selectedId: selectedId ?? rolls[0]?.localId,
    submitResult,
    customerOptions: toCustomerOptions(customerPage?.records ?? []),
    warehouseOptions: toOptions(warehousePage?.records ?? [], 'warehouseName'),
    creatingDraft,
    savingBase,
    savingRolls: savingRolls || importingRolls,
    updatingRolls,
    savingWorkbench: savingPlan || savingPlanBatch || previewingPlan,
    submitting,
    setCurrent,
    setRolls,
    setSelectedId,
    handleBaseNext,
    handleConfigNext,
    handleImportPreview,
    handlePlanChange,
    handlePreviewPlan,
    handleProcessNext,
    handleRollsNext,
    handleSavePlan,
    handleSavePlanBatch,
    handleSubmit,
  }
}

function toOptions<T extends { uuid: string }>(items: T[], labelKey: keyof T) {
  return items.map((item) => ({ label: String(item[labelKey]), value: item.uuid }))
}

function toCustomerOptions(items: Customer[]) {
  return items.map((item) => ({
    label: item.customerName,
    value: item.uuid,
    defaultInvoice: item.defaultInvoice,
    settleDay: item.settleDay,
    settleType: item.settleType,
    taxRate: item.taxRate,
  }))
}

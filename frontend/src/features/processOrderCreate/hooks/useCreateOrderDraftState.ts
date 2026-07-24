import { useEffect, useRef, useState } from 'react'
import type {
  DraftOrderBaseDTO,
  DraftOrderVO,
  PlanPreviewVO,
  ProcessOrderSubmitVO,
  ProcessPlanDTO,
  ProcessRoutePreviewDTO,
  ProcessRoutePreviewVO,
} from '../../../types/processOrder'
import { hydrateDraftState } from '../createOrderState'
import { newRollDraft } from '../draftMappers'
import {
  clearCreateOrderLocalDraft,
  loadCreateOrderLocalDraft,
  saveCreateOrderLocalDraft,
} from '../localDraftStorage'
import type { RollDraft } from '../types'

interface UseCreateOrderDraftStateOptions {
  draft?: DraftOrderVO
  draftUuid?: string
  resetLocalDraft: boolean
}

export interface CreateOrderDraftSnapshot {
  baseInfo?: DraftOrderBaseDTO
  configuredPlanIds: string[]
  current: number
  plans: Record<string, ProcessPlanDTO>
  previews: Record<string, PlanPreviewVO>
  routePreviews: Record<string, ProcessRoutePreviewVO>
  routes: Record<string, ProcessRoutePreviewDTO>
  rolls: RollDraft[]
  selectedId?: string
}

export function useCreateOrderDraftState(options: UseCreateOrderDraftStateOptions) {
  const { draft, draftUuid, resetLocalDraft } = options
  const [localDraft] = useState(() => (
    draftUuid || resetLocalDraft ? undefined : loadCreateOrderLocalDraft()
  ))
  const hydratedDraftUuid = useRef<string>()
  const [current, setCurrent] = useState(() => localDraft?.current ?? 0)
  const [orderUuid, setOrderUuid] = useState<string | undefined>(() => localDraft?.orderUuid)
  const [draftVersion, setDraftVersion] = useState(() => localDraft?.orderVersion ?? 1)
  const [baseInfo, setBaseInfo] = useState<DraftOrderBaseDTO | undefined>(() => localDraft?.baseInfo)
  const [rolls, setRolls] = useState<RollDraft[]>(() => localDraft?.rolls ?? [newRollDraft()])
  const [selectedId, setSelectedId] = useState<string | undefined>(() => localDraft?.selectedId)
  const [plans, setPlans] = useState<Record<string, ProcessPlanDTO>>(() => localDraft?.plans ?? {})
  const [configuredPlanIds, setConfiguredPlanIds] = useState<string[]>(() => localDraft?.configuredPlanIds ?? [])
  const [previews, setPreviews] = useState<Record<string, PlanPreviewVO>>(() => localDraft?.previews ?? {})
  const [routes, setRoutes] = useState<Record<string, ProcessRoutePreviewDTO>>(() => localDraft?.routes ?? {})
  const [routePreviews, setRoutePreviews] = useState<Record<string, ProcessRoutePreviewVO>>(
    () => localDraft?.routePreviews ?? {},
  )
  const [submitResult, setSubmitResult] = useState<ProcessOrderSubmitVO>()

  const captureSnapshot = (): CreateOrderDraftSnapshot => structuredClone({
    baseInfo, configuredPlanIds, current, plans, previews, routePreviews, routes, rolls, selectedId,
  })
  const restoreSnapshot = (snapshot: CreateOrderDraftSnapshot) => {
    setBaseInfo(snapshot.baseInfo)
    setConfiguredPlanIds(snapshot.configuredPlanIds)
    setCurrent(snapshot.current)
    setPlans(snapshot.plans)
    setPreviews(snapshot.previews)
    setRoutePreviews(snapshot.routePreviews)
    setRoutes(snapshot.routes)
    setRolls(snapshot.rolls)
    setSelectedId(snapshot.selectedId)
  }

  useEffect(() => {
    if (resetLocalDraft) clearCreateOrderLocalDraft()
  }, [resetLocalDraft])

  useEffect(() => {
    if (draftUuid || resetLocalDraft || submitResult) return
    saveCreateOrderLocalDraft({
      baseInfo,
      current,
      orderUuid,
      orderVersion: draftVersion,
      plans,
      configuredPlanIds,
      previews,
      routePreviews,
      routes,
      rolls,
      selectedId,
    })
  }, [baseInfo, configuredPlanIds, current, draftUuid, draftVersion, orderUuid, plans, previews,
    resetLocalDraft, routePreviews, routes, rolls, selectedId, submitResult])

  useEffect(() => {
    if (!draftUuid || !draft || hydratedDraftUuid.current === draftUuid) return
    const state = hydrateDraftState(draft)
    setOrderUuid(state.orderUuid ?? draftUuid)
    setBaseInfo(state.baseInfo)
    setRolls(state.rolls)
    setPlans(state.plans)
    setConfiguredPlanIds(state.configuredPlanIds)
    setPreviews(state.previews)
    setRoutes(state.routes)
    setRoutePreviews(state.routePreviews)
    setSelectedId(state.selectedId)
    setCurrent(state.current)
    setDraftVersion(draft.order?.version ?? 1)
    hydratedDraftUuid.current = draftUuid
  }, [draft, draftUuid])

  useEffect(() => {
    if (!draftUuid || !draft || hydratedDraftUuid.current !== draftUuid) return
    const state = hydrateDraftState(draft)
    setRoutes(state.routes)
    setRoutePreviews(state.routePreviews)
  }, [draft, draftUuid])

  return {
    baseInfo,
    current,
    draftVersion,
    orderUuid,
    plans,
    configuredPlanIds,
    previews,
    routePreviews,
    routes,
    rolls,
    selectedId,
    submitResult,
    captureSnapshot,
    restoreSnapshot,
    setBaseInfo,
    setCurrent,
    setDraftVersion,
    setOrderUuid,
    setPlans,
    setConfiguredPlanIds,
    setPreviews,
    setRoutePreviews,
    setRoutes,
    setRolls,
    setSelectedId,
    setSubmitResult,
  }
}

export type CreateOrderDraftState = ReturnType<typeof useCreateOrderDraftState>

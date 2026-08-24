import { useCallback, useEffect, useRef, useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'
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
import { useVersionedRollState } from './useVersionedRollState'

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

interface HydrateDraftOptions {
  preserveCurrentStep?: boolean
}

export function useCreateOrderDraftState(options: UseCreateOrderDraftStateOptions) {
  const { draft, draftUuid, resetLocalDraft } = options
  const [localDraft] = useState(() => (
    draftUuid || resetLocalDraft ? undefined : loadCreateOrderLocalDraft()
  ))
  const hydratedDraftUuid = useRef<string | undefined>(undefined)
  const [current, setCurrent] = useState(() => localDraft?.current ?? 0)
  const [orderUuid, setOrderUuid] = useState<string | undefined>(() => localDraft?.orderUuid)
  const [draftVersion, setDraftVersionState] = useState(() => localDraft?.orderVersion ?? 1)
  const draftVersionRef = useRef(draftVersion)
  const setDraftVersion: Dispatch<SetStateAction<number>> = useCallback((update) => {
    const next = typeof update === 'function' ? update(draftVersionRef.current) : update
    draftVersionRef.current = next
    setDraftVersionState(next)
  }, [])
  const getDraftVersion = useCallback(() => draftVersionRef.current, [])
  const [baseInfo, setBaseInfo] = useState<DraftOrderBaseDTO | undefined>(() => localDraft?.baseInfo)
  const { getRollsRevision, rolls, setRolls } = useVersionedRollState(
    localDraft?.rolls ?? [newRollDraft()],
  )
  const [selectedId, setSelectedId] = useState<string | undefined>(() => localDraft?.selectedId)
  const [plans, setPlans] = useState<Record<string, ProcessPlanDTO>>(() => localDraft?.plans ?? {})
  const [configuredPlanIds, setConfiguredPlanIds] = useState<string[]>(() => localDraft?.configuredPlanIds ?? [])
  const [previews, setPreviews] = useState<Record<string, PlanPreviewVO>>(() => localDraft?.previews ?? {})
  const [routes, setRoutes] = useState<Record<string, ProcessRoutePreviewDTO>>(() => localDraft?.routes ?? {})
  const [routePreviews, setRoutePreviews] = useState<Record<string, ProcessRoutePreviewVO>>(
    () => localDraft?.routePreviews ?? {},
  )
  const [submitResult, setSubmitResult] = useState<ProcessOrderSubmitVO>()

  const hydrateDraft = useCallback((value: DraftOrderVO, hydrateOptions: HydrateDraftOptions = {}) => {
    const next = hydrateDraftState(value)
    setOrderUuid(next.orderUuid ?? value.order?.uuid)
    setBaseInfo(next.baseInfo)
    setRolls(next.rolls)
    setPlans(next.plans)
    setConfiguredPlanIds(next.configuredPlanIds)
    setPreviews(next.previews)
    setRoutes(next.routes)
    setRoutePreviews(next.routePreviews)
    setSelectedId(next.selectedId)
    if (!hydrateOptions.preserveCurrentStep) setCurrent(next.current)
    setDraftVersion(value.order?.version ?? 1)
  }, [setDraftVersion, setRolls])

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
    hydrateDraft(draft)
    hydratedDraftUuid.current = draftUuid
  }, [draft, draftUuid, hydrateDraft])

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
    getDraftVersion,
    getRollsRevision,
    hydrateDraft,
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

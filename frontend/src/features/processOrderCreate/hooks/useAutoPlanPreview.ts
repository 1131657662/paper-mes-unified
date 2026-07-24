import { useEffect, useRef } from 'react'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../types'

interface Options {
  orderUuid?: string
  selected?: RollDraft
  selectedPlan?: ProcessPlanDTO
  onPreviewPlan: (roll: RollDraft, plan: ProcessPlanDTO) => Promise<void>
}

export function useAutoPlanPreview({ orderUuid, selected, selectedPlan, onPreviewPlan }: Options) {
  const latestRef = useRef({ onPreviewPlan, selected, selectedPlan })
  latestRef.current = { onPreviewPlan, selected, selectedPlan }
  const planFingerprint = selectedPlan ? JSON.stringify(selectedPlan) : ''
  const selectedLocalId = selected?.localId
  const selectedUuid = selected?.uuid

  useEffect(() => {
    if (!orderUuid || !selectedUuid || !planFingerprint) return
    const timer = window.setTimeout(() => {
      const latest = latestRef.current
      if (!latest.selected?.uuid || !latest.selectedPlan) return
      latest.onPreviewPlan(latest.selected, latest.selectedPlan).catch(() => undefined)
    }, 700)
    return () => window.clearTimeout(timer)
  }, [orderUuid, selectedLocalId, selectedUuid, planFingerprint])
}

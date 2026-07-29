import axios from 'axios'
import { useCallback, useEffect, useRef, useState } from 'react'
import { BizError } from '../../../api/request'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../types'

interface Options {
  orderUuid?: string
  selected?: RollDraft
  selectedPlan?: ProcessPlanDTO
  onPreviewPlan: (roll: RollDraft, plan: ProcessPlanDTO, signal?: AbortSignal) => Promise<void>
}

export function useAutoPlanPreview({ orderUuid, selected, selectedPlan, onPreviewPlan }: Options) {
  const latestRef = useRef({ onPreviewPlan, selected, selectedPlan })
  latestRef.current = { onPreviewPlan, selected, selectedPlan }
  const controllerRef = useRef<AbortController | undefined>(undefined)
  const requestRef = useRef(0)
  const [error, setError] = useState<{ localId: string; text: string }>()
  const [pendingLocalId, setPendingLocalId] = useState<string>()
  const planFingerprint = selectedPlan ? JSON.stringify(selectedPlan) : ''
  const selectedLocalId = selected?.localId
  const selectedUuid = selected?.uuid
  const previewNow = useCallback(async () => {
    const latest = latestRef.current
    if (!latest.selected?.uuid || !latest.selectedPlan) return
    controllerRef.current?.abort()
    const controller = new AbortController()
    const requestId = ++requestRef.current
    controllerRef.current = controller
    setPendingLocalId(latest.selected.localId)
    try {
      await latest.onPreviewPlan(latest.selected, latest.selectedPlan, controller.signal)
      if (requestId === requestRef.current) setError(undefined)
    } catch (previewError) {
      if (!controller.signal.aborted && requestId === requestRef.current) {
        setError({ localId: latest.selected.localId, text: previewErrorText(previewError) })
      }
    } finally {
      if (requestId === requestRef.current) setPendingLocalId(undefined)
    }
  }, [])

  useEffect(() => {
    if (!orderUuid || !selectedUuid || !planFingerprint) return
    const timer = window.setTimeout(() => void previewNow(), 700)
    return () => {
      window.clearTimeout(timer)
      controllerRef.current?.abort()
    }
  }, [orderUuid, planFingerprint, previewNow, selectedLocalId, selectedUuid])

  return {
    error: error && error.localId === selectedLocalId ? error.text : undefined,
    previewing: pendingLocalId === selectedLocalId,
    previewNow,
  }
}

export function previewErrorText(error: unknown): string {
  if (error instanceof BizError && error.message) return error.message
  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
      return '预览请求超时，请重试'
    }
    if (error.response) return '预览服务暂时不可用，请重试'
    return '无法连接预览服务，请检查网络后重试'
  }
  if (error instanceof Error && error.message) return error.message
  return '预览请求失败，请重试'
}

import { useEffect, useRef, useState } from 'react'
import { previewRewindPlan } from '../../api/processOrder'
import type { FinishPreviewVO, RewindPlanPreviewDTO } from '../../types/processOrder'

interface Options {
  enabled: boolean
  orderUuid: string
  rollUuid: string
  plan: RewindPlanPreviewDTO
  onPreview: (preview: FinishPreviewVO) => void
}

interface PreviewState {
  preview: FinishPreviewVO | null
  previewing: boolean
}

interface CurrentRef<T> {
  current: T
}

interface PreviewRequestContext {
  latestRef: CurrentRef<Options>
  requestRef: CurrentRef<number>
  setPreview: (value: FinishPreviewVO | null) => void
  setPreviewing: (value: boolean) => void
}

export function useRewindingPlanPreview(options: Options): PreviewState {
  const [preview, setPreview] = useState<FinishPreviewVO | null>(null)
  const [previewing, setPreviewing] = useState(false)
  const requestRef = useRef(0)
  const latestRef = useRef(options)
  latestRef.current = options
  const planFingerprint = JSON.stringify(options.plan)
  const { enabled, orderUuid, rollUuid } = options

  useEffect(() => {
    if (!enabled) return undefined
    const requestId = ++requestRef.current
    setPreviewing(true)
    const timer = window.setTimeout(() => {
      void requestPreview({ latestRef, requestRef, setPreview, setPreviewing }, requestId)
    }, 450)
    return () => {
      window.clearTimeout(timer)
      if (requestRef.current === requestId) requestRef.current += 1
    }
  }, [enabled, orderUuid, planFingerprint, rollUuid])

  return { preview, previewing }
}

async function requestPreview(
  context: PreviewRequestContext,
  requestId: number,
): Promise<void> {
  const { onPreview, orderUuid, plan, rollUuid } = context.latestRef.current
  try {
    const result = await previewRewindPlan(orderUuid, rollUuid, plan)
    if (context.requestRef.current === requestId) {
      context.setPreview(result)
      onPreview(result)
    }
  } catch {
    if (context.requestRef.current === requestId) context.setPreview(null)
  } finally {
    if (context.requestRef.current === requestId) context.setPreviewing(false)
  }
}

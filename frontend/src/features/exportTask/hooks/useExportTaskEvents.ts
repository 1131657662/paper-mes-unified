import { useEffect } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { exportTaskKeys } from '../queries/exportTaskKeys'

export const EXPORT_TASK_EVENT_FALLBACK_INTERVAL_MS = 10_000

export function useExportTaskEvents(enabled: boolean) {
  const queryClient = useQueryClient()
  useEffect(() => {
    if (!enabled) return undefined
    const eventSource = new EventSource('/api/export-tasks/events')
    let fallbackTimer: number | undefined
    const refresh = () => {
      void queryClient.invalidateQueries({ queryKey: exportTaskKeys._def })
    }
    const stopFallback = () => {
      if (fallbackTimer === undefined) return
      window.clearInterval(fallbackTimer)
      fallbackTimer = undefined
    }
    const startFallback = () => {
      if (fallbackTimer !== undefined) return
      refresh()
      fallbackTimer = window.setInterval(refresh, EXPORT_TASK_EVENT_FALLBACK_INTERVAL_MS)
    }
    eventSource.addEventListener('export-task', refresh)
    eventSource.addEventListener('refresh', refresh)
    eventSource.addEventListener('open', stopFallback)
    eventSource.addEventListener('error', startFallback)
    return () => {
      stopFallback()
      eventSource.removeEventListener('export-task', refresh)
      eventSource.removeEventListener('refresh', refresh)
      eventSource.removeEventListener('open', stopFallback)
      eventSource.removeEventListener('error', startFallback)
      eventSource.close()
    }
  }, [enabled, queryClient])
}

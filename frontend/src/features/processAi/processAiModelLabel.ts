import type { ProcessAiStatus } from './types'

export function processAiModelLabel(status?: ProcessAiStatus): string | undefined {
  if (!status) return undefined
  if (status.providerConfigured) {
    return status.fallbackConfigured
      ? `${status.model} · ${status.fallbackModel} 兜底`
      : status.model
  }
  return status.fallbackConfigured ? `${status.fallbackModel} 兜底` : undefined
}

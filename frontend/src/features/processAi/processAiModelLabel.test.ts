import { describe, expect, it } from 'vitest'
import { processAiModelLabel } from './processAiModelLabel'
import type { ProcessAiStatus } from './types'

describe('process AI model label', () => {
  it('shows both primary and fallback when both are configured', () => {
    expect(processAiModelLabel(status(true, true)))
      .toBe('deepseek-v4-pro · glm-4.7-flash 兜底')
  })

  it('does not present an unavailable primary as configured', () => {
    expect(processAiModelLabel(status(false, true))).toBe('glm-4.7-flash 兜底')
  })

  it('hides the label when no provider is configured', () => {
    expect(processAiModelLabel(status(false, false))).toBeUndefined()
  })
})

function status(primary: boolean, fallback: boolean): ProcessAiStatus {
  return {
    enabled: true,
    ready: primary || fallback,
    provider: 'DEEPSEEK',
    model: 'deepseek-v4-pro',
    providerConfigured: primary,
    fallbackProvider: 'ZHIPU',
    fallbackModel: 'glm-4.7-flash',
    fallbackConfigured: fallback,
    messageEncryptionReady: true,
    projectMemoryState: 'READY',
  }
}

import { describe, expect, it } from 'vitest'
import { processAiAvailability } from './processAiAvailability'
import type { ProcessAiStatus } from './types'

describe('process AI assistant availability', () => {
  it('requires a saved order before opening a session', () => {
    expect(processAiAvailability(undefined, readyStatus()).unavailable).toBe('请先完成母卷录入')
  })

  it('keeps the entry actionable when the status request fails', () => {
    const availability = processAiAvailability('order-1', undefined, true)

    expect(availability.unavailable).toBeUndefined()
    expect(availability.hint).toContain('状态检查失败')
  })

  it('surfaces a missing HMAC as a configuration reason', () => {
    const availability = processAiAvailability('order-1', {
      ...readyStatus(),
      ready: false,
      unavailableReason: 'AI_MEMORY_REFERENCE_HMAC_UNAVAILABLE',
    })

    expect(availability.unavailable).toBe('AI_MEMORY_REFERENCE_HMAC_UNAVAILABLE')
    expect(availability.hint).toContain('共享记忆保护密钥未配置')
  })

  it('allows the transient memory-unavailable state to be retried from the drawer', () => {
    const availability = processAiAvailability('order-1', {
      ...readyStatus(),
      ready: false,
      unavailableReason: 'AI_MEMORY_UNAVAILABLE',
    })

    expect(availability.unavailable).toBeUndefined()
  })

  it('allows a ready assistant', () => {
    expect(processAiAvailability('order-1', readyStatus()).unavailable).toBeUndefined()
  })
})

function readyStatus(): ProcessAiStatus {
  return {
    enabled: true,
    ready: true,
    provider: 'DEEPSEEK',
    model: 'deepseek-v4-pro',
    providerConfigured: true,
    fallbackProvider: 'ZHIPU',
    fallbackModel: 'glm-4.7-flash',
    fallbackConfigured: true,
    messageEncryptionReady: true,
    projectMemoryState: 'READY',
  }
}

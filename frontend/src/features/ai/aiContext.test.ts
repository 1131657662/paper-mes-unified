import { describe, expect, it } from 'vitest'
import { buildAiContextEpoch, buildAiPageTemplate } from './aiContext'

describe('AI page context', () => {
  it('exposes only the route module as the page template', () => {
    expect(buildAiPageTemplate('/process-orders/order-secret')).toBe('process-orders')
    expect(buildAiPageTemplate('/')).toBe('dashboard')
  })

  it('uses an opaque router key without encoding the business URL', () => {
    expect(buildAiContextEpoch('abc_123')).toBe('abc-123')
    expect(buildAiContextEpoch('')).toBe('default')
  })
})

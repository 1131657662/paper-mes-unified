import { describe, expect, it } from 'vitest'
import { clampAssistantPosition } from './useDraggableAssistantWindow'

describe('clampAssistantPosition', () => {
  it('keeps the entire assistant inside the viewport', () => {
    const result = clampAssistantPosition(
      { x: 1115, y: 900 },
      { width: 1529, height: 1272 },
      { width: 580, height: 680 },
    )

    expect(result).toEqual({ x: 941, y: 584 })
  })

  it('keeps the assistant away from the top and left edges', () => {
    const result = clampAssistantPosition(
      { x: -200, y: -100 },
      { width: 1280, height: 800 },
      { width: 580, height: 680 },
    )

    expect(result).toEqual({ x: 8, y: 8 })
  })
})

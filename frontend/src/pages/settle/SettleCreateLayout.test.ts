/// <reference types="node" />

import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(new URL('./SettleCreatePage.css', import.meta.url), 'utf8')

describe('settlement create layout', () => {
  it('keeps the candidate table usable on short desktop viewports', () => {
    expect(css).toContain('height: max(720px, calc(100vh - 126px))')
    expect(css).toContain('overflow: visible')
    expect(css).toContain('min-height: 220px')
  })
})

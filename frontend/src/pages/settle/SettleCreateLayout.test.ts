/// <reference types="node" />

import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(new URL('./SettleCreatePage.css', import.meta.url), 'utf8')

describe('settlement create layout', () => {
  it('keeps the submit bar visible while the candidate table scrolls', () => {
    expect(css).toContain('grid-template-rows: auto auto minmax(240px, 1fr) auto')
    expect(css).toMatch(/\.settle-create-page__selection\s*\{[^}]*overflow:\s*hidden/s)
    expect(css).toMatch(/\.settle-create-footer\s*\{[^}]*position:\s*sticky/s)
    expect(css).toMatch(/\.settle-create-footer\s*\{[^}]*bottom:\s*0/s)
  })
})

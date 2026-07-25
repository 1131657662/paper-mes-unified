/// <reference types="node" />

import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(new URL('./SettleCreatePage.css', import.meta.url), 'utf8')

describe('settlement create layout', () => {
  it('keeps the submit bar visible while the candidate table scrolls', () => {
    expect(css).toContain('grid-template-rows: auto auto minmax(240px, 1fr) auto')
    expect(css).toMatch(/\.document-module-page\.settle-create-page\s*\{[^}]*display:\s*grid/s)
    expect(css).toMatch(/\.settle-create-page__selection\s*\{[^}]*overflow:\s*hidden/s)
    expect(css).toMatch(/\.settle-create-footer\s*\{[^}]*position:\s*sticky/s)
    expect(css).toMatch(/\.settle-create-footer\s*\{[^}]*bottom:\s*0/s)
  })

  it('keeps candidate space allocated on narrow screens', () => {
    const narrowPageRule = css.match(/@media \(max-width: 860px\) \{\s*\.document-module-page\.settle-create-page \{([\s\S]*?)\n  \}/)

    expect(css).toContain('height: max(600px, calc(100dvh - 126px))')
    expect(narrowPageRule?.[1]).not.toContain('height:')
  })
})

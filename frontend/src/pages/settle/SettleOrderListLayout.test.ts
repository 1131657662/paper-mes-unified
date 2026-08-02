/// <reference types="node" />

import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(new URL('./SettleOrderList.css', import.meta.url), 'utf8')
const page = readFileSync(new URL('./SettleOrderList.tsx', import.meta.url), 'utf8')

describe('settlement order list layout', () => {
  it('uses the shell summary slot before toolbar in document mode', () => {
    expect(page).toContain("summary={viewMode === 'documents' ? <SettleListSummary summary={summary} /> : undefined}")
    expect(page).not.toContain(') : <SettleListSummary summary={summary} />}')
  })

  it('freezes the table header whenever rows are present', () => {
    expect(page).toContain("fixedHeader={tableDensity !== 'empty'}")
    expect(css).toMatch(/\.document-page-table \.settle-order-table \.ant-table-header\s*\{[^}]*background:\s*var\(--mes-color-soft\)/s)
  })
})

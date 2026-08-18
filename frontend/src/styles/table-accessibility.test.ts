import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(new URL('./table-accessibility.css', import.meta.url), 'utf8')

describe('表格语义标签无障碍样式', () => {
  it.each([
    ['success', '#237804'],
    ['warning', '#874d00'],
    ['error', '#a8071a'],
  ])('%s 状态使用高对比度文字色', (status, color) => {
    expect(css).toContain(`.mes-data-tag.ant-tag-${status}`)
    expect(css).toMatch(new RegExp(`ant-tag-${status}[\\s\\S]*?color: ${color} !important;`))
  })
})
